package com.tissue.issue.adapter.in.web.dto.request;

import org.springframework.lang.Nullable;

public record UpdateStoryPointRequest(
	@Nullable Integer storyPoint
) {
}
