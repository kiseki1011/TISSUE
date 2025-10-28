package com.tissue.api.issue.domain.enums;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum IssueRelationType {
	RELEVANT(false),
	BLOCKS(true),
	// BLOCKED_BY(true),
	CAUSES(true),
	// CAUSED_BY(true),
	DUPLICATES(true);
	// DUPLICATED_BY(true);

	private final boolean requiresAcyclicCheck;

	public boolean requiresAcyclicCheck() {
		return this.requiresAcyclicCheck;
	}

	// public IssueRelationType getOpposite() {
	// 	return switch (this) {
	// 		case BLOCKS -> BLOCKED_BY;
	// 		case BLOCKED_BY -> BLOCKS;
	// 		case CAUSES -> CAUSED_BY;
	// 		case CAUSED_BY -> CAUSES;
	// 		case DUPLICATES -> DUPLICATED_BY;
	// 		case DUPLICATED_BY -> DUPLICATES;
	// 		case RELEVANT -> RELEVANT;
	// 	};
	// }
}
