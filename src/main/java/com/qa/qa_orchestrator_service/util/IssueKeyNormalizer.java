package com.qa.qa_orchestrator_service.util;

/**
 * IssueKeyNormalizer
 *
 * Normalizes any issue key format to PROJ-N.
 *
 * Examples:
 *   "project-4"   → "PROJ-4"
 *   "PROJECT-4"   → "PROJ-4"
 *   "proj-4"      → "PROJ-4"
 *   "PROJ-4"      → "PROJ-4"
 *   "project 4"   → "PROJ-4"
 *   "proj4"       → "PROJ-4"
 */
public class IssueKeyNormalizer {

    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) return raw;

        String upper = raw.toUpperCase().trim();

        if (upper.matches("PROJ-\\d+")) {
            return upper;
        }

        String digits = upper.replaceAll("[^0-9]", "");

        if (!digits.isEmpty()) {
            return "PROJ-" + digits;
        }

        upper = upper.replaceAll("\\s+", "-");
        upper = upper.replaceAll("[^A-Z0-9\\-]", "");
        upper = upper.replaceAll("-{2,}", "-");
        return upper;
    }
}