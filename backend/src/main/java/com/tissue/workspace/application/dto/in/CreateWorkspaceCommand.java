package com.tissue.workspace.application.dto.in;

public record CreateWorkspaceCommand(String name, String description, Long memberId) {}
