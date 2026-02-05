package com.tissue.issue.application.dto.request;

import java.util.Map;

public record UpdateCustomFieldsCommand(
        String issueKey, Map<Long, Object> customFields) {}
