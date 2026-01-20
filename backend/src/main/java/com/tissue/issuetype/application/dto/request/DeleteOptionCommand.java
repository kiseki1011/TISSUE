package com.tissue.issuetype.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record DeleteOptionCommand(
        Long issueTypeId, Long issueFieldId, Long optionId, ProjectMemberContext actorContext) {}
