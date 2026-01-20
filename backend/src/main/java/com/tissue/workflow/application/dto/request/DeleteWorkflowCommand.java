package com.tissue.workflow.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record DeleteWorkflowCommand(Long workflowId, ProjectMemberContext actorContext) {}
