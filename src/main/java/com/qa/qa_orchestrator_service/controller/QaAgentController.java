package com.qa.qa_orchestrator_service.controller;

import com.qa.qa_orchestrator_service.model.QaAnalysisResult;
import com.qa.qa_orchestrator_service.model.QaAnalyzeRequest;
import com.qa.qa_orchestrator_service.service.QaOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/qa/api/v1/agent")
public class QaAgentController {

    private final QaOrchestratorService service;

    // In-memory cache: issueKey -> cached result + timestamp
    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    private static class CachedResult {
        final QaAnalysisResult result;
        final Instant cachedAt;
        CachedResult(QaAnalysisResult result) {
            this.result = result;
            this.cachedAt = Instant.now();
        }
        boolean isExpired() {
            return Instant.now().isAfter(cachedAt.plusSeconds(CACHE_TTL_SECONDS));
        }
    }

    public QaAgentController(QaOrchestratorService service) {
        this.service = service;
    }

    private QaAnalysisResult getOrAnalyze(String issueKey) {
        String key = issueKey.toUpperCase();
        CachedResult cached = cache.get(key);
        if (cached != null && !cached.isExpired()) {
            return cached.result;
        }
        QaAnalysisResult result = service.runAnalysis(issueKey);
        cache.put(key, new CachedResult(result));
        return result;
    }

    @PostMapping(value = "/requirements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getRequirements(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult r = getOrAnalyze(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Requirement Analysis — ").append(r.getTraceabilityId()).append("\n\n");
        sb.append("Status: ").append(safe(r.getRequirementStatus())).append("\n");
        sb.append("Feature: ").append(safe(r.getFeatureSummary())).append("\n\n");
        appendList(sb, "Clarified Requirements", r.getClarifiedRequirements());
        appendList(sb, "Edge Cases", r.getEdgeCases());
        appendList(sb, "Open Questions", r.getOpenQuestions());
        appendList(sb, "Scope", r.getScope());
        appendList(sb, "Out of Scope", r.getOutOfScope());
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/testcases", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getTestCases(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult r = getOrAnalyze(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("🧪 Test Cases — ").append(r.getTraceabilityId()).append("\n\n");
        appendList(sb, "Test Scenarios", r.getTestScenarios());
        if (r.getTestCases() != null && !r.getTestCases().isEmpty()) {
            sb.append("Test Cases:\n");
            r.getTestCases().forEach(tc -> {
                sb.append("\n").append(tc.getId()).append(": ").append(tc.getTitle()).append("\n");
                sb.append("  Type: ").append(safe(tc.getTestType()))
                  .append(" | Suite: ").append(safe(tc.getSuiteTag()))
                  .append(" | Priority: ").append(safe(tc.getPriority())).append("\n");
                sb.append("  Preconditions: ").append(safe(tc.getPreconditions())).append("\n");
                if (tc.getSteps() != null) {
                    sb.append("  Steps:\n");
                    for (int i = 0; i < tc.getSteps().size(); i++) {
                        sb.append("    ").append(i + 1).append(". ").append(tc.getSteps().get(i)).append("\n");
                    }
                }
                sb.append("  Expected: ").append(safe(tc.getExpectedResult())).append("\n");
            });
        }
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/risk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getRisk(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult r = getOrAnalyze(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ Risk Analysis — ").append(r.getTraceabilityId()).append("\n\n");
        sb.append("Risk Score: ").append(r.getRiskScore() != null ? r.getRiskScore() : "N/A").append("\n");
        sb.append("Risk Level: ").append(safe(r.getRiskLevel())).append("\n");
        sb.append("Risk Reason: ").append(safe(r.getRiskReason())).append("\n\n");
        appendList(sb, "Top Risk Drivers", r.getTopRiskDrivers());
        sb.append("Release Recommendation: ").append(safe(r.getReleaseRecommendation())).append("\n");
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/automation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getAutomation(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult r = getOrAnalyze(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 Automation Strategy — ").append(r.getTraceabilityId()).append("\n\n");
        sb.append("Recommendation: ").append(safe(r.getAutomationRecommendation())).append("\n");
        sb.append("Reasoning: ").append(safe(r.getAutomationReasoning())).append("\n");
        sb.append("Coverage Split: ").append(safe(r.getCoverageSplit())).append("\n");
        sb.append("Framework: ").append(safe(r.getFrameworkSuggestion())).append("\n");
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/bugreport", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getBugReport(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult r = getOrAnalyze(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("🐛 Bug Report — ").append(r.getTraceabilityId()).append("\n\n");

        var stages = r.getStages();
        var bugReport = stages != null ? stages.getBugReport() : null;

        if (bugReport != null) {
            sb.append("Title: ").append(safe(bugReport.getTitle())).append("\n");
            sb.append("Environment: ").append(safe(bugReport.getEnvironment())).append("\n");
            sb.append("Severity: ").append(safe(bugReport.getSeverity())).append("\n");
            sb.append("Priority: ").append(safe(bugReport.getPriority())).append("\n\n");
            if (bugReport.getReproductionSteps() != null) {
                sb.append("Reproduction Steps:\n");
                for (int i = 0; i < bugReport.getReproductionSteps().size(); i++) {
                    sb.append("  ").append(i + 1).append(". ")
                      .append(bugReport.getReproductionSteps().get(i)).append("\n");
                }
            }
            sb.append("\nExpected Result: ").append(safe(bugReport.getExpectedResult())).append("\n");
            sb.append("Actual Result: To be filled by QA engineer after test execution.\n\n");
            if (bugReport.getAffectedAreas() != null) {
                sb.append("Affected Areas:\n");
                bugReport.getAffectedAreas().forEach(a -> sb.append("  - ").append(a).append("\n"));
            }
            sb.append("Suggested Assignee: ").append(safe(bugReport.getSuggestedAssignee())).append("\n");
        } else {
            sb.append("Bug report not available for this issue.");
        }
        return Map.of("result", sb.toString().trim());
    }

    private void appendList(StringBuilder sb, String title, List<String> items) {
        sb.append(title).append(":\n");
        if (items == null || items.isEmpty()) {
            sb.append("  None identified.\n");
        } else {
            items.forEach(item -> sb.append("  - ").append(item).append("\n"));
        }
        sb.append("\n");
    }

    private String safe(String value) {
        return value != null ? value : "Not specified";
    }
}