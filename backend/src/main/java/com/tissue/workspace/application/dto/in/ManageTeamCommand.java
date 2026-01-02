package com.tissue.workspace.application.dto.in;

public record ManageTeamCommand(String workspaceKey, Long targetMemberId, Long teamId) {}
