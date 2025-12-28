package com.tissue.issue.domain.util;

public final class IssueKeyUtil {

    private static final String KEY_SEPARATOR = "-";

    private IssueKeyUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static String extractProjectKey(String issueKey) {
        if (issueKey == null || issueKey.isEmpty()) {
            throw new IllegalArgumentException("Issue key must not be null or empty");
        }

        int separatorIndex = issueKey.indexOf(KEY_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == issueKey.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid issue key format: '%s'.".formatted(issueKey));
        }

        return issueKey.substring(0, separatorIndex);
    }
}
