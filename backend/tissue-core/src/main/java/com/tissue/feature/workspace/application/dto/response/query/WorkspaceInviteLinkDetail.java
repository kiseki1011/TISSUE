package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record WorkspaceInviteLinkDetail(
        String workspaceKey,
        String workspaceName,
        List<String> projectKeys,
        String creatorDisplayName,
        @Nullable String creatorEmail,
        boolean isValid) {

    public static WorkspaceInviteLinkDetail of(WorkspaceInviteLink link, WorkspaceMember linkCreator) {
        return WorkspaceInviteLinkDetail.builder()
                .workspaceKey(link.getWorkspaceKey())
                .workspaceName(link.getWorkspace().getName())
                .projectKeys(link.getProjectKeys())
                .creatorDisplayName(linkCreator.getDisplayName())
                .creatorEmail(linkCreator.getMember().getEmail())
                .isValid(link.isValid())
                .build();
    }
}
