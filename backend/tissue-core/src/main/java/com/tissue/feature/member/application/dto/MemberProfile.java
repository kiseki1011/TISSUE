package com.tissue.feature.member.application.dto;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.organization.position.application.dto.response.PositionSummary;
import com.tissue.feature.organization.team.application.dto.response.TeamSummary;
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

        @Schema(description = "Assigned position") @Nullable PositionSummary position,

        @Schema(description = "Assigned team") @Nullable TeamSummary team,

        Instant joinedAt,
        Instant lastUpdatedAt) {
    public static MemberProfile from(Member member) {
        return MemberProfile.builder()
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .role(member.getRole())
                .position(member.getPosition() == null ? null : PositionSummary.from(member.getPosition()))
                .team(member.getTeam() == null ? null : TeamSummary.from(member.getTeam()))
                .joinedAt(member.getCreatedAt())
                .lastUpdatedAt(member.getLastModifiedAt())
                .build();
    }
}
