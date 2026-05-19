package com.tissue.feature.workspace.application.dto.response.query;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.time.Instant;
import java.util.List;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record InvitationDetail(
        Long invitationId,
        String workspaceKey,
        String workspaceName,
        WorkspaceRole workspaceRole,
        List<String> projectKeys,
        String inviterName,
        String inviterEmail,
        Instant invitedAt) {

    private static final String UNKNOWN = "UNKNOWN";

    /**
     * {@code inviter} may be {@code null}.
     * <ul>
     *  <li>{@code invitation.createdBy} is {@code null} (the sender is the system
     *  with no auth context)</li>
     *  <li>the inviter is no longer active (withdrawn after sending)</li>
     * </ul>
     * Even when {@code inviter} is present, {@link Member#getEmail()} may be
     * {@code null} (members created via the no-email signup).
     */
    public static InvitationDetail from(Invitation invitation, @Nullable Member inviter) {
        String name = (inviter != null) ? inviter.getName() : UNKNOWN;
        String email = (inviter != null && inviter.getEmail() != null) ? inviter.getEmail() : UNKNOWN;

        return InvitationDetail.builder()
                .invitationId(invitation.getId())
                .workspaceKey(invitation.getWorkspaceKey())
                .workspaceName(invitation.getWorkspace().getName())
                .workspaceRole(invitation.getWorkspaceRole())
                .projectKeys(invitation.getProjectKeys())
                .inviterName(name)
                .inviterEmail(email)
                .invitedAt(invitation.getCreatedAt())
                .build();
    }
}
