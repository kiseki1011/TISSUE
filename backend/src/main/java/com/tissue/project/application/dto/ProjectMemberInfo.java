package com.tissue.project.application.dto;

import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.enums.ProjectRole;

public record ProjectMemberInfo(
        Long projectMemberId, Long memberId, String projectKey, String workspaceKey, ProjectRole role) {

    public static ProjectMemberInfo from(ProjectMember projectMember) {
        return new ProjectMemberInfo(
                projectMember.getId(),
                projectMember.getMemberId(),
                projectMember.getProjectKey(),
                projectMember.getWorkspaceKey(),
                projectMember.getRole());
    }
}
