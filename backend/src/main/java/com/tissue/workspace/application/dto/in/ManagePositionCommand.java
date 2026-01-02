package com.tissue.workspace.application.dto.in;

public record ManagePositionCommand(String workspaceKey, Long targetMemberId, Long positionId) {}
