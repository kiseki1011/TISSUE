package com.tissue.feature.project.application.dto.response;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import java.util.Collection;
import java.util.List;

public record ProjectMembersCommandResult(String workspaceKey, String projectKey, List<Long> memberIds, int totalSize) {
    public static ProjectMembersCommandResult of(Project project, Collection<ProjectMember> projectMembers) {
        List<Long> ids = projectMembers.stream().map(ProjectMember::getMemberId).toList();

        return new ProjectMembersCommandResult(project.getWorkspaceKey(), project.getKey(), ids, ids.size());
    }
}
