package com.tissue.workspace.application.dto.in;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record TransferOwnershipCommand(Long targetMemberId, WorkspaceMemberContext actorContext) {}
