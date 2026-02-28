package com.tissue.feature.issue.domain.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

public class IssueKeyExtractor {
    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\b[A-Za-z][A-Za-z0-9]+-\\d+\\b");

    @Nullable
    public static String extract(@Nullable String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = ISSUE_KEY_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }
        return null;
    }
}
