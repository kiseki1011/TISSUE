package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.member.domain.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * A global member who can be added to a project (not already an active member of it),
 * returned by the candidate search a manager uses to pick people to add.
 */
public record MemberCandidateSummary(
        Long memberId,
        String username,
        String displayName,

        @Schema(description = "Email address (`null` if `email-required` is disabled)") @Nullable
        String email) {

    public static MemberCandidateSummary from(Member member) {
        return new MemberCandidateSummary(member.getId(), member.getUsername(), member.getName(), member.getEmail());
    }
}
