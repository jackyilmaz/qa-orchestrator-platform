package com.qa.qa_orchestrator_service.controller;

import com.qa.qa_orchestrator_service.model.QaAnalysisResult;
import com.qa.qa_orchestrator_service.model.QaAnalyzeRequest;
import com.qa.qa_orchestrator_service.service.QaOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * QaAgentController — Copilot Studio dedicated endpoints
 *
 * Each endpoint runs the full pipeline but returns ONLY the relevant stage
 * as plain formatted text. Copilot agents display clean, focused output.
 * Existing endpoints are NOT modified.
 */
@RestController
@RequestMapping("/qa/api/v1/agent")
public class QaAgentController {

    private final QaOrchestratorService service;

    public QaAgentController(QaOrchestratorService service) {
        this.service = service;
    }

    @PostMapping(value = "/requirements", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getRequirements(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult result = runPipeline(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("📋 Requirement Analysis — ").append(result.getTraceabilityId()).append("\n\n");
        sb.append("Status: ").append(safe(result.getRequirementStatus())).append("\n");
        sb.append("Feature: ").append(safe(result.getFeatureSummary())).append("\n\n");
        appendList(sb, "Clarified Requirements", result.getClarifiedRequirements());
        appendList(sb, "Edge Cases", result.getEdgeCases());
        appendList(sb, "Open Questions", result.getOpenQuestions());
        appendList(sb, "Scope", result.getScope());
        appendList(sb, "Out of Scope", result.getOutOfScope());
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/testcases", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getTestCases(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult result = runPipeline(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("🧪 Test Cases — ").append(result.getTraceabilityId()).append("\n\n");
        appendList(sb, "Test Scenarios", result.getTestScenarios());
        if (result.getTestCases() != null && !result.getTestCases().isEmpty()) {
            sb.append("Test Cases:\n");
            result.getTestCases().forEach(tc -> {
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
        QaAnalysisResult result = runPipeline(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("⚠️ Risk Analysis — ").append(result.getTraceabilityId()).append("\n\n");
        sb.append("Risk Score: ").append(result.getRiskScore() != null ? result.getRiskScore() : "N/A").append("\n");
        sb.append("Risk Level: ").append(safe(result.getRiskLevel())).append("\n");
        sb.append("Risk Reason: ").append(safe(result.getRiskReason())).append("\n\n");
        appendList(sb, "Top Risk Drivers", result.getTopRiskDrivers());
        sb.append("Release Recommendation: ").append(safe(result.getReleaseRecommendation())).append("\n");
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/automation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getAutomation(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult result = runPipeline(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 Automation Strategy — ").append(result.getTraceabilityId()).append("\n\n");
        sb.append("Recommendation: ").append(safe(result.getAutomationRecommendation())).append("\n");
        sb.append("Reasoning: ").append(safe(result.getAutomationReasoning())).append("\n");
        sb.append("Coverage Split: ").append(safe(result.getCoverageSplit())).append("\n");
        sb.append("Framework: ").append(safe(result.getFrameworkSuggestion())).append("\n");
        return Map.of("result", sb.toString().trim());
    }

    @PostMapping(value = "/bugreport", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getBugReport(@RequestBody QaAnalyzeRequest request) {
        QaAnalysisResult result = runPipeline(request.getIssueKey());
        String bugSection = extractSection(result.getRawOutput(), "STAGE 5 — BUG REPORT TEMPLATE");
        StringBuilder sb = new StringBuilder();
        sb.append("🐛 Bug Report — ").append(result.getTraceabilityId()).append("\n\n");
        if (bugSection != null && !bugSection.isBlank()) {
            sb.append(bugSection.trim());
        } else {
            sb.append("Bug report not available for this issue.");
        }
        return Map.of("result", sb.toString().trim());
    }

    private QaAnalysisResult runPipeline(String issueKey) {
        String raw = service.runAnalysis(issueKey);
        return service.buildStructuredAnalysis(issueKey, raw);
    }

    private String extractSection(String raw, String sectionHeader) {
        if (raw == null || sectionHeader == null) return null;
        int start = raw.indexOf(sectionHeader);
        if (start == -1) return null;
        int end = raw.indexOf("STAGE 6", start);
        if (end == -1) end = raw.indexOf("========", start + sectionHeader.length());
        if (end == -1) end = raw.length();
        return raw.substring(start + sectionHeader.length(), end).trim();
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