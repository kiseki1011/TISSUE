package com.tissue.project.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.domain.enums.ProjectRole;
import lombok.Builder;

@Builder
public record ChangeProjectRoleCommand(Long targetMemberId, ProjectRole grantRole, ProjectMemberContext actor) {}
