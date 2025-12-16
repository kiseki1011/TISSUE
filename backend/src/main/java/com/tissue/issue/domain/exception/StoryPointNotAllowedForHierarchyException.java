package com.tissue.issue.domain.exception;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.issue.domain.enums.IssueHierarchy;

public class StoryPointNotAllowedForHierarchyException extends BadRequestException {

	public StoryPointNotAllowedForHierarchyException(IssueHierarchy hierarchy) {
		super("Story points can only be set or modifiable for %s. Current hierarchy is '%s'"
			.formatted(IssueHierarchy.getStoryPointModifiable(), hierarchy));

		addContext("issueHierarchy", hierarchy);
		addContext("modifiableHierarchies", IssueHierarchy.getStoryPointModifiable());
	}
}
