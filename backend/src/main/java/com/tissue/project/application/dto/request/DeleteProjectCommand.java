package com.tissue.project.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record DeleteProjectCommand(String projectKey, WorkspaceMemberContext actor) {}
