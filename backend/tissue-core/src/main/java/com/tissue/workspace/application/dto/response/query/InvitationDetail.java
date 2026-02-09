package com.tissue.workspace.application.dto.response.query;

import com.tissue.member.domain.Member;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.enums.InvitationStatus;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record InvitationDetail(
        Long invitationId,
        String workspaceKey,
        String workspaceName,
        List<String> projectKeys,
        String inviterName,
        String inviterEmail,
        InvitationStatus status,
        Instant invitedAt) {

    public static InvitationDetail from(Invitation invitation, @Nullable Member inviter) {
        String name = (inviter != null) ? inviter.getName() : "UNKNOWN";
        String email = (inviter != null) ? inviter.getEmail() : "UNKNOWN";

        return InvitationDetail.builder()
                .invitationId(invitation.getId())
                .workspaceKey(invitation.getWorkspaceKey())
                .workspaceName(invitation.getWorkspace().getName())
                .projectKeys(invitation.getProjectKeys())
                .inviterName(name)
                .inviterEmail(email)
                .status(invitation.getStatus())
                .invitedAt(invitation.getCreatedAt())
                .build();
    }
}
