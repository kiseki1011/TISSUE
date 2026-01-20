package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record KickWorkspaceMemberCommand(Long targetMemberId, WorkspaceMemberContext actorContext) {}
