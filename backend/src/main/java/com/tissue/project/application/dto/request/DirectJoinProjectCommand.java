package com.tissue.project.application.dto.request;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;

public record DirectJoinProjectCommand(String workspaceKey, String projectKey, WorkspaceMemberInfo actor) {}
