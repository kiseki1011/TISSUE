package com.tissue.workspace.application.dto.in;

public record TransferOwnershipCommand(
        String workspaceKey, Long targetMemberId, Long actorMemberId) {}
