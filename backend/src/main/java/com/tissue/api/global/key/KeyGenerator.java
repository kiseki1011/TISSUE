package com.tissue.api.global.key;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyGenerator {

	public static String generateWorkspaceKey() {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.WORKSPACE, WorkspaceKeyGenerator.generateWorkspaceKey());
	}

	public static String generateIssueKey(String prefix, long issueNumber) {
		return KeyPrefixPolicy.format(prefix, issueNumber);
	}

	public static String generateSprintKey(long sprintNumber) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.SPRINT, sprintNumber);
	}
}
