package com.tissue.issue.application.dto.response.info;

import com.tissue.project.domain.ProjectMember;

public record ParticipantInfo(
        Long memberId, String username, String displayName, boolean archived) {
    public static ParticipantInfo from(ProjectMember pm) {
        if (pm == null) {
            return null;
        }
        return new ParticipantInfo(
                pm.getWorkspaceMember().getMember().getId(),
                pm.getWorkspaceMember().getMember().getUsername(),
                pm.getWorkspaceMember().getDisplayName(),
                pm.isArchived());
    }
}
