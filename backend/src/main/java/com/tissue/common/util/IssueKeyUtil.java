package com.tissue.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public abstract class IssueKeyUtil {

	private static final String KEY_SEPARATOR = "-";

	public static String extractProjectKey(String issueKey) {
		if (issueKey == null || issueKey.isEmpty()) {
			throw new IllegalArgumentException("Issue key must not be null or empty.");
		}

		int separatorIndex = issueKey.indexOf(KEY_SEPARATOR);

		if (separatorIndex <= 0 || separatorIndex == issueKey.length() - 1) {
			throw new IllegalArgumentException("Invalid issue key format. Issue key was '%s'." + issueKey);
		}

		return issueKey.substring(0, separatorIndex);
	}
}
