package com.tissue.workspace.application.dto.response.query;

import com.tissue.workspace.application.dto.ProjectJoinConfigDto;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;
import java.util.List;
import lombok.Builder;

@Builder
public record WorkspaceInviteLinkDetail(
        String workspaceKey,
        String workspaceName,
        List<ProjectJoinConfigDto> projectConfigs,
        String creatorDisplayName,
        String creatorEmail,
        boolean isValid) {

    public static WorkspaceInviteLinkDetail of(WorkspaceInviteLink link, WorkspaceMember linkCreator) {
        return WorkspaceInviteLinkDetail.builder()
                .workspaceKey(link.getWorkspaceKey())
                .workspaceName(link.getWorkspace().getName())
                .projectConfigs(link.getProjectConfigs().stream()
                        .map(config -> new ProjectJoinConfigDto(config.projectKey(), config.role()))
                        .toList())
                .creatorDisplayName(linkCreator.getDisplayName())
                .creatorEmail(linkCreator.getMember().getEmail())
                .isValid(link.isValid())
                .build();
    }
}
