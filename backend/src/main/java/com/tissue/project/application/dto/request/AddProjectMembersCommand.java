package com.tissue.project.application.dto.request;

import com.tissue.project.domain.enums.ProjectRole;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public record AddProjectMembersCommand(
        String workspaceKey, String projectKey, List<ProjectMemberConfig> targetMembers) {
    public record ProjectMemberConfig(Long memberId, ProjectRole projectRole) {}

    public Set<Long> extractMemberIds() {
        return targetMembers.stream()
                .map(ProjectMemberConfig::memberId)
                .collect(Collectors.toSet());
    }

    public Map<Long, ProjectRole> extractRoleMap() {
        return targetMembers.stream()
                .collect(
                        Collectors.toMap(
                                ProjectMemberConfig::memberId, ProjectMemberConfig::projectRole));
    }
}
