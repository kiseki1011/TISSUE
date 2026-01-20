package com.tissue.project.application.dto.request;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import lombok.Builder;

@Builder
public record CreateProjectCommand(String projectKey, String title, String description, WorkspaceMemberContext actor) {}
