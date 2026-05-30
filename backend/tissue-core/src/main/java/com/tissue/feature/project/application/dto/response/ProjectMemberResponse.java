package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.ProjectMember;

public record ProjectMemberResponse(String projectKey, Long memberId) {
    public static ProjectMemberResponse of(ProjectMember projectMember) {
        return new ProjectMemberResponse(projectMember.getProjectKey(), projectMember.getMemberId());
    }
}
