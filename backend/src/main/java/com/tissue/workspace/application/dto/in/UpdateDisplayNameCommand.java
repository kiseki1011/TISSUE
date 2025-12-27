package com.tissue.workspace.application.dto.in;

public record UpdateDisplayNameCommand(String workspaceKey, Long memberId, String displayName) {}
