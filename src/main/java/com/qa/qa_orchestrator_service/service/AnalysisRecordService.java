package com.qa.qa_orchestrator_service.service;

import com.qa.qa_orchestrator_service.model.QaAnalysisResult;
import com.qa.qa_orchestrator_service.repository.AnalysisRecord;
import com.qa.qa_orchestrator_service.repository.AnalysisRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisRecordService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisRecordService.class);
    private final AnalysisRecordRepository repository;

    public AnalysisRecordService(AnalysisRecordRepository repository) {
        this.repository = repository;
    }

    public void save(QaAnalysisResult result, long pipelineDurationMs) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            record.setIssueKey(result.getTraceabilityId());
            record.setFeatureSummary(result.getFeatureSummary());
            record.setRequirementStatus(result.getRequirementStatus());
            record.setRiskLevel(result.getRiskLevel());
            record.setRiskScore(result.getRiskScore());
            record.setReleaseRecommendation(result.getReleaseRecommendation());
            record.setAutomationRecommendation(result.getAutomationRecommendation());
            record.setTestCaseCount(result.getTestCases() != null ? result.getTestCases().size() : 0);
            record.setAnalyzedAt(Instant.now());
            record.setPipelineDurationMs(pipelineDurationMs);
            repository.save(record);
            log.info("[DB] Analysis record saved for {} | risk={} | score={} | testCases={}",
                    result.getTraceabilityId(), result.getRiskLevel(),
                    result.getRiskScore(), record.getTestCaseCount());
        } catch (Exception e) {
            log.warn("[DB] Failed to save analysis record for {}: {}",
                    result.getTraceabilityId(), e.getMessage());
        }
    }

    public void markAsCompleted(String issueKey, String releaseSummary) {
    try {
        List<AnalysisRecord> history = repository.findByIssueKeyOrderByAnalyzedAtDesc(issueKey);
        if (history.isEmpty()) {
            log.warn("[DB] No analysis record found for {} to mark as completed", issueKey);
            return;
        }

        AnalysisRecord target = history.get(0);

        // If the latest record has no risk data, find the most recent one that does
        if (target.getRiskLevel() == null) {
            target = history.stream()
                .filter(r -> r.getRiskLevel() != null)
                .findFirst()
                .orElse(history.get(0));
            log.warn("[DB] Latest record for {} had null riskLevel, using record from {}",
                issueKey, target.getAnalyzedAt());
        }

        target.setCompletedAt(Instant.now());
        target.setReleaseSummary(releaseSummary);
        repository.save(target);
        log.info("[DB] Marked {} as completed | riskLevel={} | riskScore={}",
            issueKey, target.getRiskLevel(), target.getRiskScore());

    } catch (Exception e) {
        log.warn("[DB] Failed to mark {} as completed: {}", issueKey, e.getMessage());
    }
}

    public List<AnalysisRecord> getRecentAnalyses() {
        return repository.findTop10ByOrderByAnalyzedAtDesc();
    }

    public List<AnalysisRecord> getHistoryForIssue(String issueKey) {
        return repository.findByIssueKeyOrderByAnalyzedAtDesc(issueKey);
    }

    public List<AnalysisRecord> getHighRiskTickets() {
        return repository.findByRiskLevelOrderByRiskScoreDesc("HIGH");
    }

    public List<AnalysisRecord> getBlockedTickets() {
        return repository.findByReleaseRecommendationOrderByAnalyzedAtDesc("Block");
    }

    public List<AnalysisRecord> getReleasedTickets() {
        return repository.findByCompletedAtIsNotNullOrderByCompletedAtDesc();
    }

    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new HashMap<>();
        long total = repository.count();
        Double avgRisk = repository.findAverageRiskScore();
        List<AnalysisRecord> blocked = repository.findByReleaseRecommendationOrderByAnalyzedAtDesc("Block");
        List<Object[]> mostAnalyzed = repository.findMostAnalyzedIssues();

        Map<String, Long> riskDistribution = new HashMap<>();
        riskDistribution.put("HIGH", 0L);
        riskDistribution.put("MEDIUM", 0L);
        riskDistribution.put("LOW", 0L);
        for (Object[] row : repository.countByRiskLevel()) {
            String level = (String) row[0];
            Long count = (Long) row[1];
            if (level != null) riskDistribution.put(level, count);
        }

        Map<String, Long> releaseDistribution = new HashMap<>();
        releaseDistribution.put("Block", 0L);
        releaseDistribution.put("Caution", 0L);
        releaseDistribution.put("Go", 0L);
        for (Object[] row : repository.countByReleaseRecommendation()) {
            String rec = (String) row[0];
            Long count = (Long) row[1];
            if (rec != null) releaseDistribution.put(rec, count);
        }

        summary.put("totalAnalyses", total);
        summary.put("averageRiskScore", avgRisk != null ? Math.round(avgRisk) : 0);
        summary.put("highRiskCount", riskDistribution.get("HIGH"));
        summary.put("mediumRiskCount", riskDistribution.get("MEDIUM"));
        summary.put("lowRiskCount", riskDistribution.get("LOW"));
        summary.put("riskDistribution", riskDistribution);
        summary.put("releaseDistribution", releaseDistribution);
        summary.put("blockedCount", blocked.size());
        summary.put("releasedCount", repository.findByCompletedAtIsNotNullOrderByCompletedAtDesc().size());
        summary.put("mostAnalyzedIssues", mostAnalyzed.stream()
                .limit(5)
                .map(row -> Map.of("issueKey", row[0], "count", row[1]))
                .toList());

        return summary;
    }
}