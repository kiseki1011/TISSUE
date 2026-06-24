package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record ProjectMemberSummary(
        Long memberId,
        String username,
        String displayName,
        ProjectRole role,
        boolean active,
        Instant joinedAt,

        @Schema(description = "Email address (`null` if `email-required` is disabled)") @Nullable
        String email,

        SystemRole systemRole) {

    public static ProjectMemberSummary from(ProjectMember pm) {
        return new ProjectMemberSummary(
                pm.getMemberId(),
                pm.getMember().getUsername(),
                pm.getDisplayName(),
                pm.getRole(),
                !pm.isSoftDeleted(),
                pm.getCreatedAt(),
                pm.getMember().getEmail(),
                pm.getMember().getRole());
    }
}
