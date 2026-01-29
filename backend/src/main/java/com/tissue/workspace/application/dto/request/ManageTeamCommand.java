package com.tissue.workspace.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record ManageTeamCommand(Long targetMemberId, Long teamId, WorkspaceMemberContext actorContext) {}
