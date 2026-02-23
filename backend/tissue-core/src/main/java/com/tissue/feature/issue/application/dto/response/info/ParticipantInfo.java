package com.tissue.feature.issue.application.dto.response.info;

import com.tissue.feature.project.domain.ProjectMember;
import org.jspecify.annotations.Nullable;

public record ParticipantInfo(@Nullable Long memberId, String username, String displayName) {

    public static ParticipantInfo from(@Nullable ProjectMember projectMember) {
        if (projectMember == null) {
            return new ParticipantInfo(null, "", "");
        }
        return new ParticipantInfo(
                projectMember.getWorkspaceMember().getMember().getId(),
                projectMember.getWorkspaceMember().getMember().getUsername(),
                projectMember.getWorkspaceMember().getDisplayName());
    }
}
