package com.tissue.api.issue.domain.util;

import com.tissue.api.issue.domain.model.enums.KeyPrefix;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class KeyGenerator {

	public static String generateIssueTypeKey(long id) {
		return KeyPrefix.CUSTOM_ISSUE_TYPE.prefix() + "_" + id;
	}

	public static String generateFieldKey(long id) {
		return KeyPrefix.CUSTOM_FIELD.prefix() + "_" + id;
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
