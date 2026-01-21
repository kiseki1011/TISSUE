package com.tissue.project.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record KickProjectMemberCommand(Long targetMemberId, ProjectMemberContext actorContext) {}
