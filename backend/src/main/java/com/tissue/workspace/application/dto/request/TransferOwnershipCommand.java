package com.tissue.workspace.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record TransferOwnershipCommand(Long targetMemberId, WorkspaceMemberContext actorContext) {}
