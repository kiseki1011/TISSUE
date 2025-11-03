package com.tissue.api.issuetype.domain.enums;

import lombok.Getter;

@Getter
public enum DefaultIssueType {
	EPIC("Epic"),
	STORY("Story"),
	TASK("Task"),
	BUG("Bug"),
	SUB_TASK("Sub-task");

	private final String label;

	DefaultIssueType(String label) {
		this.label = label;
	}
}
