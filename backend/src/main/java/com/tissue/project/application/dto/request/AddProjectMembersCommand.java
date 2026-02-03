package com.tissue.project.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.Set;

public record AddProjectMembersCommand(Set<Long> targetMemberIds, ProjectMemberContext actorContext) {}
