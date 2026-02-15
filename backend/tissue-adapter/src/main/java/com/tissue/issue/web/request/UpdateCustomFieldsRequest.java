package com.tissue.issue.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record UpdateCustomFieldsRequest(@NotEmpty @NotNull Map<Long, Object> customFields) {}
