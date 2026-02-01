package com.tissue.issuetype.application.dto.request;

import com.tissue.global.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record RenameIssueTypeCommand(
        String workspaceKey, String projectKey, Long issueTypeId, Name name, ProjectMemberContext actorContext) {}
