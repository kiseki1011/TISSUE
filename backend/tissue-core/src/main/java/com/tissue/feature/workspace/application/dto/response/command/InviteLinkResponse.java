package com.tissue.feature.workspace.application.dto.response.command;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record InviteLinkResponse(
        String token, String fullUrl, @Nullable Instant expiredAt) {}
