package com.tissue.api.project.adapter.in.web.dto.request;

import com.tissue.api.project.domain.enums.ProjectRole;

public record ChangeProjectRoleRequest(
	ProjectRole newProjectRole
) {
}
