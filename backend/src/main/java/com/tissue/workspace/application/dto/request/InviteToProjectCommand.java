package com.tissue.workspace.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.domain.enums.ProjectRole;
import java.util.Set;

public record InviteToProjectCommand(Set<String> emails, ProjectRole role, ProjectMemberContext actorContext) {}
