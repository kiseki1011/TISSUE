package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;

public record ExpireLinkCommand(String workspaceKey, String token, WorkspaceMemberInfo actor) {}
