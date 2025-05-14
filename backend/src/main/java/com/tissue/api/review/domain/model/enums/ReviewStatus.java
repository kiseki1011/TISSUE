package com.tissue.api.review.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReviewStatus {
	// PENDING,
	COMMENT,
	APPROVED,
	CHANGES_REQUESTED
}
