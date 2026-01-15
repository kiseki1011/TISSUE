package com.tissue.project.application.dto.request;

import com.tissue.project.domain.enums.ProjectRole;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import lombok.Builder;

@Builder
public record ChangeProjectRoleCommand(
        String workspaceKey, String projectKey, Long targetMemberId, ProjectRole newRole, WorkspaceMemberInfo actor) {}
