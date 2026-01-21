package com.tissue.issuetype.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.List;
import lombok.Builder;

@Builder
public record ReorderOptionsCommand(
        Long issueTypeId, Long issueFieldId, List<Long> targetOrderedIds, ProjectMemberContext actorContext) {}
