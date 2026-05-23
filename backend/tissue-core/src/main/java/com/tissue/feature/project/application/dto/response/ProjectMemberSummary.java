package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import java.time.Instant;

public record ProjectMemberSummary(
        Long memberId, String username, String displayName, ProjectRole role, boolean active, Instant joinedAt) {

    public static ProjectMemberSummary from(ProjectMember pm) {
        var wm = pm.getWorkspaceMember();
        return new ProjectMemberSummary(
                pm.getMemberId(),
                wm.getMember().getUsername(),
                wm.getDisplayName(),
                pm.getRole(),
                !pm.isSoftDeleted(),
                pm.getCreatedAt());
    }
}
