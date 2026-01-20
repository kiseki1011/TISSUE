package com.tissue.issue.application.dto.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import java.util.Map;

public record UpdateCustomFieldsCommand(
        String issueKey, Map<Long, Object> customFields, ProjectMemberContext actorContext) {}
