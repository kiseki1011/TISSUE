package com.tissue.workspace.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record UpdateDisplayNameCommand(Long targetMemberId, String displayName, WorkspaceMemberContext actorContext) {}
