package com.tissue.project.adapter.in.web.dto.request;

import com.tissue.project.domain.enums.ProjectRole;

public record ChangeProjectRoleRequest(ProjectRole newProjectRole) {}
