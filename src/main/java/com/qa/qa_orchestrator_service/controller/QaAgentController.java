package com.qa.qa_orchestrator_service.controller;

import com.qa.qa_orchestrator_service.model.QaAnalyzeRequest;
import com.qa.qa_orchestrator_service.model.QaAnalyzeResponse;
import com.qa.qa_orchestrator_service.service.QaOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

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
        String raw = service.runAnalysis(request.getIssueKey());
        String section = extractSection(raw, "STAGE 1 — REQUIREMENT ANALYSIS", "STAGE 2");
        return Map.of("result", "📋 Requirement Analysis — " + request.getIssueKey() + "\n\n" + safe(section));
    }

    @PostMapping(value = "/testcases", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getTestCases(@RequestBody QaAnalyzeRequest request) {
        String raw = service.runAnalysis(request.getIssueKey());
        String section = extractSection(raw, "STAGE 2 — TEST STRATEGY", "STAGE 3");
        return Map.of("result", "🧪 Test Cases — " + request.getIssueKey() + "\n\n" + safe(section));
    }

    @PostMapping(value = "/risk", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getRisk(@RequestBody QaAnalyzeRequest request) {
        String raw = service.runAnalysis(request.getIssueKey());
        String section = extractSection(raw, "STAGE 6 — RISK ANALYSIS", "========");
        return Map.of("result", "⚠️ Risk Analysis — " + request.getIssueKey() + "\n\n" + safe(section));
    }

    @PostMapping(value = "/automation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getAutomation(@RequestBody QaAnalyzeRequest request) {
        String raw = service.runAnalysis(request.getIssueKey());
        String section = extractSection(raw, "STAGE 3 — AUTOMATION DECISION", "STAGE 4");
        if (section == null) section = extractSection(raw, "STAGE 3 — AUTOMATION DECISION", "STAGE 5");
        return Map.of("result", "🤖 Automation Strategy — " + request.getIssueKey() + "\n\n" + safe(section));
    }

    @PostMapping(value = "/bugreport", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> getBugReport(@RequestBody QaAnalyzeRequest request) {
        String raw = service.runAnalysis(request.getIssueKey());
        String section = extractSection(raw, "STAGE 5 — BUG REPORT TEMPLATE", "STAGE 6");
        return Map.of("result", "🐛 Bug Report — " + request.getIssueKey() + "\n\n" + safe(section));
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

    private String safe(String value) {
        return value != null ? value : "Not available.";
    }
}