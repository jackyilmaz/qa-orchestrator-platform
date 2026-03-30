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
 * Returns only the relevant pipeline stage as clean formatted text.
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
        QaAnalysisResult r = service.runAnalysis(request.getIssueKey());
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
        QaAnalysisResult r = service.runAnalysis(request.getIssueKey());
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
        QaAnalysisResult r = service.runAnalysis(request.getIssueKey());
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
        QaAnalysisResult r = service.runAnalysis(request.getIssueKey());
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
        QaAnalysisResult r = service.runAnalysis(request.getIssueKey());
        StringBuilder sb = new StringBuilder();
        sb.append("🐛 Bug Report — ").append(r.getTraceabilityId()).append("\n\n");
        String section = extractSection(r.getRawOutput(), "STAGE 5", "STAGE 6");
        if (section != null && !section.isBlank()) {
            sb.append(section.trim());
        } else {
            sb.append("Bug report not available for this issue.");
        }
        return Map.of("result", sb.toString().trim());
    }

    private String extractSection(String raw, String start, String end) {
        if (raw == null) return null;
        int s = raw.indexOf(start);
        if (s == -1) return null;
        s += start.length();
        int e = raw.indexOf(end, s);
        if (e == -1) e = raw.length();
        return raw.substring(s, e).trim();
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