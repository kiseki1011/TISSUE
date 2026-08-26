package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberType;
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

        SystemRole systemRole,

        @Schema(description = "Whether this member is a human or an agent")
        MemberType memberType,

        @Schema(description = "For an agent, the owning member's username (`null` for a human)") @Nullable
        String ownerUsername,

        @Schema(description = "For an agent, the owning member's display name (`null` for a human)") @Nullable
        String ownerName) {

    public static ProjectMemberSummary from(ProjectMember pm) {
        Member member = pm.getMember();
        Member owner = member.getOwner();
        return new ProjectMemberSummary(
                pm.getMemberId(),
                member.getUsername(),
                pm.getDisplayName(),
                pm.getRole(),
                !pm.isSoftDeleted(),
                pm.getCreatedAt(),
                member.getEmail(),
                member.getRole(),
                member.getMemberType(),
                owner == null ? null : owner.getUsername(),
                owner == null ? null : owner.getName());
    }
}
