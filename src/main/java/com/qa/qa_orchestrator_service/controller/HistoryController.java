package com.qa.qa_orchestrator_service.controller;

import com.qa.qa_orchestrator_service.repository.AnalysisRecord;
import com.qa.qa_orchestrator_service.service.AnalysisRecordService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qa")
public class HistoryController {

    private final AnalysisRecordService analysisRecordService;

    public HistoryController(AnalysisRecordService analysisRecordService) {
        this.analysisRecordService = analysisRecordService;
    }

    @GetMapping("/api/v1/history")
    public ResponseEntity<List<AnalysisRecord>> getRecentHistory() {
        return ResponseEntity.ok(analysisRecordService.getRecentAnalyses());
    }

    @GetMapping("/api/v1/history/{issueKey}")
    public ResponseEntity<List<AnalysisRecord>> getHistoryForIssue(@PathVariable String issueKey) {
        return ResponseEntity.ok(analysisRecordService.getHistoryForIssue(issueKey.toUpperCase()));
    }

    @GetMapping("/api/v1/intelligence/high-risk")
    public ResponseEntity<List<AnalysisRecord>> getHighRiskTickets() {
        return ResponseEntity.ok(analysisRecordService.getHighRiskTickets());
    }

    @GetMapping("/api/v1/intelligence/blocked")
    public ResponseEntity<List<AnalysisRecord>> getBlockedTickets() {
        return ResponseEntity.ok(analysisRecordService.getBlockedTickets());
    }

    @GetMapping("/api/v1/intelligence/released")
    public ResponseEntity<List<AnalysisRecord>> getReleasedTickets() {
        return ResponseEntity.ok(analysisRecordService.getReleasedTickets());
    }

    @GetMapping("/api/v1/intelligence/released/summary")
    public ResponseEntity<Map<String, Object>> getReleasedSummaryForCopilot() {
        List<AnalysisRecord> released = analysisRecordService.getReleasedTickets();

        if (released.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                "count", 0,
                "message", "No tickets have been released yet.",
                "tickets", List.of()
            ));
        }

        List<Map<String, Object>> tickets = released.stream().map(r -> {
            String verdict = r.getReleaseSummary() != null
                ? r.getReleaseSummary().split("\n")[0].replaceAll("(?i)^verdict:\\s*", "").trim()
                : "No verdict available";
            String releasedDate = r.getCompletedAt() != null
                ? r.getCompletedAt().toString().substring(0, 10)
                : "-";
            return Map.<String, Object>of(
                "issueKey", r.getIssueKey(),
                "feature", r.getFeatureSummary() != null ? r.getFeatureSummary() : "-",
                "riskLevel", r.getRiskLevel() != null ? r.getRiskLevel() : "-",
                "verdict", verdict,
                "releasedAt", releasedDate
            );
        }).toList();

        StringBuilder message = new StringBuilder();
        message.append("Released tickets (").append(released.size()).append(" total):\n\n");
        for (Map<String, Object> t : tickets) {
            message.append("📋 ").append(t.get("issueKey")).append("\n");
            message.append("Feature: ").append(t.get("feature")).append("\n");
            message.append("Risk: ").append(t.get("riskLevel")).append("\n");
            message.append("Verdict: ").append(t.get("verdict")).append("\n");
            message.append("Released: ").append(t.get("releasedAt")).append("\n\n");
        }

        return ResponseEntity.ok(Map.of(
            "count", released.size(),
            "message", message.toString().trim(),
            "tickets", tickets
        ));
    }

    @GetMapping("/api/v1/intelligence/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(analysisRecordService.getSummary());
    }
}