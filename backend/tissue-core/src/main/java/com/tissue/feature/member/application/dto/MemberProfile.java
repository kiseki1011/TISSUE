package com.tissue.feature.member.application.dto;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "Member profile information")
@Builder
public record MemberProfile(
        @Schema(
                description = "Email address (`null` if `email-required` is disabled)",
                example = "gildong@termissue.dev")
        @Nullable
        String email,

        @Schema(example = "gildong") String username,

        @Schema(example = "Gildong Hong") String name,

        @Schema(description = "System-level role for this member", example = "USER")
        SystemRole role,

        Instant joinedAt,
        Instant lastUpdatedAt) {
    public static MemberProfile from(Member member) {
        return MemberProfile.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .role(member.getRole())
                .joinedAt(member.getCreatedAt())
                .lastUpdatedAt(member.getLastModifiedAt())
                .build();
    }
}
