package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import java.util.Collection;
import java.util.List;

public record ProjectMembersResponse(String workspaceKey, String projectKey, List<Long> memberIds, int totalSize) {

    public static ProjectMembersResponse of(Project project, Collection<ProjectMember> projectMembers) {
        List<Long> ids = projectMembers.stream().map(ProjectMember::getMemberId).toList();
        return new ProjectMembersResponse(project.getWorkspaceKey(), project.getKey(), ids, ids.size());
    }
}
