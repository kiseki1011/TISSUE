package com.tissue.api.workspace.application.dto.response;

import java.time.Instant;

public record InviteLinkResponse(
	String token,
	String fullUrl,
	Instant expiredAt
) {
}
