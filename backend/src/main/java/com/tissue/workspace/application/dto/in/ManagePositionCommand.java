package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record ManagePositionCommand(Long targetMemberId, Long positionId, WorkspaceMemberContext actorContext) {}
