package com.tissue.issuetype.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;

public record DeleteIssueTypeCommand(
        String workspaceKey, String projectKey, Long issueTypeId, ProjectMemberContext actorContext) {}
