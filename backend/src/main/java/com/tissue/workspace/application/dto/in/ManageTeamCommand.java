package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record ManageTeamCommand(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext) {}
