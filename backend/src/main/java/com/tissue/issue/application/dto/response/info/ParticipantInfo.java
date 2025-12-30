package com.tissue.issue.application.dto.response.info;

import com.tissue.project.domain.ProjectMember;
import org.jspecify.annotations.Nullable;

// TODO: should i use Boolean?
public record ParticipantInfo(@Nullable Long memberId, String username, String displayName, boolean archived) {

    // TODO: needs refactor
    public static ParticipantInfo from(@Nullable ProjectMember projectMember) {
        if (projectMember == null) {
            return new ParticipantInfo(null, "", "", false);
        }
        return new ParticipantInfo(
                projectMember.getWorkspaceMember().getMember().getId(),
                projectMember.getWorkspaceMember().getMember().getUsername(),
                projectMember.getWorkspaceMember().getDisplayName(),
                projectMember.isArchived());
    }
}
