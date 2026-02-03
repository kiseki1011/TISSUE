package com.tissue.workspace.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.Set;

public record InviteToProjectCommand(Set<String> emails, ProjectMemberContext actorContext) {}
