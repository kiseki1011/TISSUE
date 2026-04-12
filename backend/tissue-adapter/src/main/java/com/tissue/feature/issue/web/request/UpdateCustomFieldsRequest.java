package com.tissue.feature.issue.web.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateCustomFieldsRequest(
        @Schema(
                description = "Map of custom field ID to its value. "
                        + "Value type must match the field's type (ex: string for TEXT, number for INTEGER).")
        @NotEmpty
        @NotNull
        @Size(max = 50)
        Map<Long, Object> customFields) {}
