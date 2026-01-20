package com.tissue.issuetype.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record DeleteIssueFieldCommand(Long issueTypeId, Long issueFieldId, ProjectMemberContext actorContext) {}
