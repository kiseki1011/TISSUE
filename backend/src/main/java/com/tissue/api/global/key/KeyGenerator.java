package com.tissue.api.global.key;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyGenerator {

	public static String generateWorkspaceKey() {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.WORKSPACE, WorkspaceKeyGenerator.generateWorkspaceKeySuffix());
	}

	public static String generateIssueKey(String prefix, long issueNumber) {
		return KeyPrefixPolicy.format(prefix.toUpperCase(), issueNumber);
	}

	public static String generateSprintKey(long sprintNumber) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.SPRINT, sprintNumber);
	}

	public static String generateIssueTypeKey(long id) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.ISSUE_TYPE, id);
	}

	public static String generateIssueFieldKey(long id) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.ISSUE_FIELD, id);
	}

	public static String generateEnumFieldOptionKey(long id) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.ISSUE_ENUM_FIELD_OPTION, id);
	}

	public static String generateWorkflowKey(long id) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.WORKFLOW, id);
	}

	public static String generateStatusKey(long id) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.STATUS, id);
	}

	public static String generateTransitionKey(long id) {
		return KeyPrefixPolicy.format(KeyPrefixPolicy.TRANSITION, id);
	}
}
