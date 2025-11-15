package com.tissue.api.issue.domain.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IssueRelationType {
	RELEVANT(false),
	BLOCKS(true),
	CAUSES(true),
	DUPLICATES(true);

	private final boolean requiresAcyclicCheck;

	public boolean requiresAcyclicCheck() {
		return this.requiresAcyclicCheck;
	}
}
