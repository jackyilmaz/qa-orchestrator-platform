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
        return ResponseEntity.ok(analysisRecordService.getReleasedSummary());
    }

    @GetMapping("/api/v1/intelligence/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(analysisRecordService.getSummary());
    }
}