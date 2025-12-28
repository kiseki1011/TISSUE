package com.tissue.workspace.application.dto.out.command;

import java.time.Instant;

public record InviteLinkResponse(String token, String fullUrl, Instant expiredAt) {}
