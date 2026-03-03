package com.tissue.feature.workspace.application.dto.response.command;

import com.tissue.feature.workspace.domain.Invitation;

public record InvitationResponse(String workspaceKey, Long invitationId) {
    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(invitation.getWorkspaceKey(), invitation.getId());
    }
}
