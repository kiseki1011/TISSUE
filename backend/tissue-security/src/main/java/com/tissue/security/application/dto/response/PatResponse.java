package com.tissue.security.application.dto.response;

import com.tissue.security.domain.PatScope;
import com.tissue.security.domain.PersonalAccessToken;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "Personal Access Token (PAT) metadata")
@Builder
public record PatResponse(
        Long id,
        String name,
        PatScope scope,
        @Nullable Instant expiresAt,
        @Nullable Instant lastUsedAt,
        boolean revoked,
        Instant createdAt) {

    public static PatResponse from(PersonalAccessToken token) {
        return PatResponse.builder()
                .id(token.getId())
                .name(token.getName())
                .scope(token.getScope())
                .expiresAt(token.getExpiresAt())
                .lastUsedAt(token.getLastUsedAt())
                .revoked(token.isRevoked())
                .createdAt(token.getCreatedAt())
                .build();
    }
}
