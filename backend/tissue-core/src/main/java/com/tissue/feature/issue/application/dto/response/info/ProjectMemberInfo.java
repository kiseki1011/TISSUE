package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.project.domain.ProjectMember;
import org.jspecify.annotations.Nullable;

public record ProjectMemberInfo(@Nullable Long memberId, String username, String displayName, boolean active) {
    public static ProjectMemberInfo from(@Nullable ProjectMember projectMember) {
        if (projectMember == null) {
            return new ProjectMemberInfo(null, "", "", false);
        }
        return new ProjectMemberInfo(
                projectMember.getWorkspaceMember().getMember().getId(),
                projectMember.getWorkspaceMember().getMember().getUsername(),
                projectMember.getWorkspaceMember().getDisplayName(),
                !projectMember.isSoftDeleted());
    }
}
