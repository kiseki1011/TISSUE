package com.tissue.issue.adapter.in.web.dto.request;

import java.util.Map;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomFieldsRequest(
	@NotEmpty @NotNull Map<Long, Object> customFields
) {
}
