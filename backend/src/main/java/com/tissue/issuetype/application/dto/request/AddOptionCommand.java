package com.tissue.issuetype.application.dto.request;

import com.tissue.common.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import lombok.Builder;

@Builder
public record AddOptionCommand(Long issueTypeId, Long issueFieldId, Name name, ProjectMemberContext actorContext) {}
