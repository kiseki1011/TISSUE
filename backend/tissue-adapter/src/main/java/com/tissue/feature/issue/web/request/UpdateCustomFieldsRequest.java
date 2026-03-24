package com.tissue.feature.issue.web.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateCustomFieldsRequest(
        @NotEmpty @NotNull @Size(max = 50) Map<Long, Object> customFields) {}
