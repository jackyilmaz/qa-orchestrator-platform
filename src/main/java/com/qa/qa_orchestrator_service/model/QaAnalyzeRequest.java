package com.qa.qa_orchestrator_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QaAnalyzeRequest {

    @NotBlank(message = "issueKey is required")
    @Size(max = 50, message = "issueKey must be 50 characters or less")
    private String issueKey;

    public String getIssueKey() {
        return issueKey;
    }

    public void setIssueKey(String issueKey) {
        if (issueKey != null) {
            this.issueKey = normalize(issueKey.trim());
        }
    }

    /**
     * Normalizes issueKey to Jira format.
     *
     * Strategy:
     * 1. Extract only the digits from the input
     * 2. Use "PROJ" as the default project prefix
     * 3. Return PROJ-{number}
     *
     * Examples:
     *   "project-4"   → "PROJ-4"
     *   "PROJECT-4"   → "PROJ-4"
     *   "proj-4"      → "PROJ-4"
     *   "PROJ-4"      → "PROJ-4"
     *   "project 4"   → "PROJ-4"
     *   "analyze project-4" → "PROJ-4"
     *   "proj4"       → "PROJ-4"
     */
    private String normalize(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String upper = raw.toUpperCase().trim();

        // If already in correct PROJ-N format, return as-is
        if (upper.matches("PROJ-\\d+")) {
            return upper;
        }

        // Extract digits from the input
        String digits = upper.replaceAll("[^0-9]", "");

        if (!digits.isEmpty()) {
            return "PROJ-" + digits;
        }

        // Fallback: uppercase and clean
        upper = upper.replaceAll("\\s+", "-");
        upper = upper.replaceAll("[^A-Z0-9\\-]", "");
        upper = upper.replaceAll("-{2,}", "-");
        return upper;
    }
}