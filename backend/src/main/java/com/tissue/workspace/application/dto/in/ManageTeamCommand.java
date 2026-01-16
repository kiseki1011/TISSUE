package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;

public record ManageTeamCommand(String workspaceKey, Long targetMemberId, Long teamId, WorkspaceMemberInfo actor) {}
