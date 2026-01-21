package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record ChangeReporterCommand(String issueKey, Long targetMemberId, ProjectMemberContext actor) {}
