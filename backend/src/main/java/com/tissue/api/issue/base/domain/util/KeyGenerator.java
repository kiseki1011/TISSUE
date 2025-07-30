package com.tissue.api.issue.base.domain.util;

import com.tissue.api.issue.base.domain.enums.KeyPrefix;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// TODO: Consider removing this class and directly combine the string
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyGenerator {

	public static String generateIssueTypeKey(long id) {
		return KeyPrefix.CUSTOM_ISSUE_TYPE.prefix() + "_" + id;
	}

	public static String generateIssueFieldKey(long id) {
		return KeyPrefix.CUSTOM_ISSUE_FIELD.prefix() + "_" + id;
	}

	public static String generateWorkflowKey(long id) {
		return KeyPrefix.CUSTOM_WORKFLOW.prefix() + "_" + id;
	}

	public static String generateStepKey(long id) {
		return KeyPrefix.CUSTOM_STEP.prefix() + "_" + id;
	}

	public static String generateTransitionKey(long id) {
		return KeyPrefix.CUSTOM_TRANSITION.prefix() + "_" + id;
	}
}
