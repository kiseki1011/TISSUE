package com.tissue.issue.domain.exception;

import static com.tissue.issue.domain.exception.IssueErrorCode.*;

import java.util.List;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.issue.domain.enums.IssueHierarchy;
import com.tissue.issue.domain.enums.IssueRelationType;

public class IssueExceptions {

	private IssueExceptions() {
	}

	public static ResourceNotFoundException notFound(String workspaceKey, String issueKey) {
		return new ResourceNotFoundException(ISSUE_NOT_FOUND)
			.addContext("workspaceKey", workspaceKey)
			.addContext("issueKey", issueKey);
	}

	public static BadRequestException invalidParentHierarchy(String workspaceKey, String parentIssueKey,
		IssueHierarchy parentHierarchy, String childIssueKey, IssueHierarchy childHierarchy) {
		return new BadRequestException(INVALID_PARENT_HIERARCHY)
			.addContext("workspaceKey", workspaceKey)
			.addContext("parentIssueKey", parentIssueKey)
			.addContext("parentHierarchy", parentHierarchy)
			.addContext("childIssueKey", childIssueKey)
			.addContext("childHierarchy", childHierarchy);
	}

	public static BadRequestException storyPointNotAllowed(String workspacekey) {
		return new BadRequestException(STORY_POINT_NOT_ALLOWED)
			.addContext("workspaceKey", )
			.addContext("issueKey", )
			.addContext("currentHierarchy", )
			.addContext("workspaceKey", )
	}

	public static BadRequestException relationCycleDetected(String sourceIssueKey, String targetIssueKey,
		IssueRelationType relationType, List<String> path) {
		return new BadRequestException(RELATION_CIRCULAR_DEPENDENCY)
			.addContext("sourceIssueKey", sourceIssueKey)
			.addContext("targetIssueKey", targetIssueKey)
			.addContext("relationType", relationType.name())
			.addContext("detectedCyclePath", path);
	}
}
