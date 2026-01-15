package com.tissue.project.application.dto.request;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
import lombok.Builder;

@Builder
public record KickProjectMemberCommand(
        String workspaceKey, String projectKey, Long targetMemberId, WorkspaceMemberInfo actor) {}
