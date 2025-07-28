package com.tissue.api.issue.domain.model.enums;

public enum KeyPrefix {

	CUSTOM_ISSUE_TYPE("custom_issue_type"),
	CUSTOM_ISSUE_FIELD("custom_field"),
	CUSTOM_WORKFLOW("custom_workflow"),
	CUSTOM_STEP("custom_step"),
	CUSTOM_TRANSITION("custom_transition");

	private final String prefix;

	KeyPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String prefix() {
		return prefix;
	}
}
