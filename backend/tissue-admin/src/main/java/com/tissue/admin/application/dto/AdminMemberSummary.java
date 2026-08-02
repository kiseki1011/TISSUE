package com.tissue.admin.application.dto;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.organization.position.application.dto.response.PositionSummary;
import com.tissue.feature.organization.team.application.dto.response.TeamSummary;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Schema(description = "Member summary for the admin member directory")
@Builder
public record AdminMemberSummary(
        @Schema(example = "42") Long id,

        @Schema(description = "Email (`null` when email-required is disabled)", example = "gildong@tissue.dev")
        @Nullable
        String email,

        @Schema(example = "gildong") String username,
        @Schema(example = "Gildong Hong") String name,

        @Schema(description = "System-level role", example = "USER")
        SystemRole role,

        @Schema(description = "Account status", example = "ACTIVE")
        MemberStatus status,

        @Schema(description = "Assigned team") @Nullable TeamSummary team,

        @Schema(description = "Assigned position") @Nullable PositionSummary position,

        Instant joinedAt,

        @Schema(description = "When the account was withdrawn, if any") @Nullable
        Instant deletedAt) {

    public static AdminMemberSummary from(Member member) {
        return AdminMemberSummary.builder()
                .id(member.getId())
                .email(member.getEmail())
                .username(member.getUsername())
                .name(member.getName())
                .role(member.getRole())
                .status(member.getStatus())
                .team(member.getTeam() == null ? null : TeamSummary.from(member.getTeam()))
                .position(member.getPosition() == null ? null : PositionSummary.from(member.getPosition()))
                .joinedAt(member.getCreatedAt())
                .deletedAt(member.getDeletedAt())
                .build();
    }
}
