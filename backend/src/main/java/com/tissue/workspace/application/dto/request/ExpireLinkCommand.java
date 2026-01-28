package com.tissue.workspace.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;

public record ExpireLinkCommand(String token, WorkspaceMemberContext actorContext) {}
