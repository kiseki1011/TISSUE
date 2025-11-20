package com.tissue.api.project.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProjectKeyRequest(
	@Size(min = 3, max = 12) @NotBlank String newKey
) {
}
