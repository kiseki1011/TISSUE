package com.tissue.feature.member.application.dto;

import com.tissue.feature.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "Member profile information")
@Builder
public record MemberProfile(
        @Schema(description = "Email address (`null` if `email-required` is disabled)", example = "user@tissue.com")
        @Nullable
        String email,

        @Schema(description = "Username", example = "johndoe")
        String username,

        @Schema(description = "Display name", example = "John Doe")
        String name,

        @Schema(description = "Account creation timestamp") Instant joinedAt,

        @Schema(description = "Last profile update timestamp")
        Instant lastUpdatedAt) {
    public static MemberProfile from(Member member) {
        return MemberProfile.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .joinedAt(member.getCreatedAt())
                .lastUpdatedAt(member.getLastModifiedAt())
                .build();
    }
}
