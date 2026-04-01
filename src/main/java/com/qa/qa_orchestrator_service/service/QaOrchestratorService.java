package com.qa.qa_orchestrator_service.service;

import com.qa.qa_orchestrator_service.jira.JiraClient;
import com.qa.qa_orchestrator_service.model.QaAnalysisResult;
import com.qa.qa_orchestrator_service.service.stage.RequirementAnalysisStage;
import com.qa.qa_orchestrator_service.service.stage.TestDesignStage;
import com.qa.qa_orchestrator_service.service.stage.AutomationDecisionStage;
import com.qa.qa_orchestrator_service.service.stage.RiskAnalysisStage;
import com.qa.qa_orchestrator_service.service.stage.BugReportStage;
import com.qa.qa_orchestrator_service.service.stage.AnalysisSummaryStage;
import com.qa.qa_orchestrator_service.service.stage.StageAggregationStage;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * QaOrchestratorService
 *
 * Orchestrates the full QA analysis pipeline with:
 * - Structured logging with stage timing
 * - Pipeline-level timeout (25 seconds)
 * - Graceful timeout response instead of hanging
 * - PostgreSQL persistence of analysis results
 */
@Service
public class QaOrchestratorService {

    private static final int PIPELINE_TIMEOUT_SECONDS = 60;

    private final JiraClient jiraClient;
    private final RequirementAnalysisStage requirementAnalysisStage;
    private final TestDesignStage testDesignStage;
    private final AutomationDecisionStage automationDecisionStage;
    private final RiskAnalysisStage riskAnalysisStage;
    private final BugReportStage bugReportStage;
    private final AnalysisSummaryStage analysisSummaryStage;
    private final StageAggregationStage stageAggregationStage;
    private final PipelineLogger pipelineLogger;
    private final AnalysisRecordService analysisRecordService;

    public QaOrchestratorService(
            JiraClient jiraClient,
            RequirementAnalysisStage requirementAnalysisStage,
            TestDesignStage testDesignStage,
            AutomationDecisionStage automationDecisionStage,
            RiskAnalysisStage riskAnalysisStage,
            BugReportStage bugReportStage,
            AnalysisSummaryStage analysisSummaryStage,
            StageAggregationStage stageAggregationStage,
            PipelineLogger pipelineLogger,
            AnalysisRecordService analysisRecordService) {
        this.jiraClient = jiraClient;
        this.requirementAnalysisStage = requirementAnalysisStage;
        this.testDesignStage = testDesignStage;
        this.automationDecisionStage = automationDecisionStage;
        this.riskAnalysisStage = riskAnalysisStage;
        this.bugReportStage = bugReportStage;
        this.analysisSummaryStage = analysisSummaryStage;
        this.stageAggregationStage = stageAggregationStage;
        this.pipelineLogger = pipelineLogger;
        this.analysisRecordService = analysisRecordService;
    }

    public QaAnalysisResult runAnalysis(String issueKey) {
        CompletableFuture<QaAnalysisResult> future = CompletableFuture.supplyAsync(
                () -> runPipeline(issueKey));

        try {
            return future.get(PIPELINE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            pipelineLogger.stageError(issueKey, "pipeline", "Pipeline timed out after " + PIPELINE_TIMEOUT_SECONDS + " seconds");
            return buildTimeoutResult(issueKey);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return buildTimeoutResult(issueKey);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new RuntimeException("Pipeline execution failed: " + cause.getMessage(), cause);
        }
    }

    private QaAnalysisResult runPipeline(String issueKey) {
        long jiraStart = System.currentTimeMillis();
        String jiraJson = jiraClient.getIssue(issueKey);
        pipelineLogger.jiraFetch(issueKey, System.currentTimeMillis() - jiraStart);

        long pipelineStart = System.currentTimeMillis();
        pipelineLogger.pipelineStart(issueKey);

        QaAnalysisResult result = new QaAnalysisResult();
        result.setTraceabilityId(issueKey);
        result.setContractVersion("v2");

        result = runStage(issueKey, "requirement", result, jiraJson,
                (r, raw) -> requirementAnalysisStage.apply(r, raw));

        if ("BLOCKED".equals(result.getRequirementStatus())) {
            pipelineLogger.blockedPipeline(issueKey);
            analysisSummaryStage.apply(result);
            stageAggregationStage.apply(result);
            long duration = System.currentTimeMillis() - pipelineStart;
            pipelineLogger.pipelineEnd(issueKey, duration,
                    result.getRequirementStatus(), result.getRiskLevel(),
                    result.getRiskScore(), result.getReleaseRecommendation());
            analysisRecordService.save(result, duration);
            jiraClient.addComment(issueKey, result.getRawOutput() != null ? result.getRawOutput() : "Pipeline blocked.");
            return result;
        }

        result = runStage(issueKey, "test_design", result, jiraJson,
                (r, raw) -> testDesignStage.apply(r, raw));
        result = runStage(issueKey, "automation", result, jiraJson,
                (r, raw) -> automationDecisionStage.apply(r, raw));
        result = runStage(issueKey, "risk", result, jiraJson,
                (r, raw) -> riskAnalysisStage.apply(r, raw));
        result = runStage(issueKey, "bug_report", result, jiraJson,
                (r, raw) -> bugReportStage.apply(r, raw));

        analysisSummaryStage.apply(result);
        stageAggregationStage.apply(result);

        long duration = System.currentTimeMillis() - pipelineStart;
        pipelineLogger.pipelineEnd(issueKey, duration,
                result.getRequirementStatus(), result.getRiskLevel(),
                result.getRiskScore(), result.getReleaseRecommendation());

        analysisRecordService.save(result, duration);

        jiraClient.addComment(issueKey, result.getRawOutput() != null ? result.getRawOutput() : "Analysis complete.");

        return result;
    }

    private QaAnalysisResult runStage(String issueKey, String stageName,
            QaAnalysisResult result, String raw, StageRunner runner) {
        long start = System.currentTimeMillis();
        pipelineLogger.stageStart(issueKey, stageName);
        try {
            runner.run(result, raw);
            pipelineLogger.stageEnd(issueKey, stageName, System.currentTimeMillis() - start);
        } catch (Exception e) {
            pipelineLogger.stageError(issueKey, stageName, e.getMessage());
            pipelineLogger.stageFallback(issueKey, stageName);
        }
        return result;
    }

    private QaAnalysisResult buildTimeoutResult(String issueKey) {
        QaAnalysisResult result = new QaAnalysisResult();
        result.setTraceabilityId(issueKey);
        result.setContractVersion("v2");
        result.setRequirementStatus("TIMEOUT");
        result.setRawOutput("Pipeline timed out after " + PIPELINE_TIMEOUT_SECONDS + " seconds. Please try again.");
        result.setAnalysisSummary("Pipeline timed out. Please try again.");
        return result;
    }

    @FunctionalInterface
    private interface StageRunner {
        void run(QaAnalysisResult result, String raw);
    }
}