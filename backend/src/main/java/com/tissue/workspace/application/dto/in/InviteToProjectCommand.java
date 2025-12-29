package com.tissue.workspace.application.dto.in;

import com.tissue.project.domain.enums.ProjectRole;
import java.util.Set;

public record InviteToProjectCommand(Set<String> emails, String workspaceKey, String projectKey, ProjectRole role) {}
