package com.tissue.api.workflow.domain.guard;

import java.util.Map;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workflow.domain.WorkflowTransition;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GuardContext {
	private final Issue issue;
	private final WorkflowTransition transition;
	private final Long actorMemberId;
	private final String projectKey;
	private final String workspaceKey;
	private final Map<String, Object> params;
}
