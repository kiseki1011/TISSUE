package com.tissue.feature.project.application.service.finder;

import com.tissue.feature.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.exception.ProjectMemberNotFoundException;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMemberFinder {

    private final ProjectMemberQueryRepository queryRepository;

    public ProjectMember getBy(Project project, Long memberId) {
        return queryRepository
                .findByProjectAndMemberId(project, memberId)
                .orElseThrow(() ->
                        new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId));
    }

    public ProjectMember getWithProjectBy(String workspaceKey, String projectKey, Long memberId) {
        return queryRepository
                .findWithProjectByKeys(workspaceKey, projectKey, memberId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(workspaceKey, projectKey, memberId));
    }

    public ProjectMember getActiveWithWorkspaceMember(String workspaceKey, String projectKey, Long memberId) {
        return queryRepository
                .findActiveWithWorkspaceMemberByKeysAndMemberId(workspaceKey, projectKey, memberId)
                .orElseThrow(() -> new ProjectMemberNotFoundException(workspaceKey, projectKey, memberId));
    }

    public Set<Long> getExistingMemberIdsBy(Project project, Collection<Long> memberIds) {
        return queryRepository.findMemberIdsByProjectAndMemberIds(project, memberIds);
    }

    public boolean existsBy(Project project, Long memberId) {
        return queryRepository.existsByProjectAndMemberId(project, memberId);
    }
}
