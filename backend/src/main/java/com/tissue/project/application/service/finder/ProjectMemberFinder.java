package com.tissue.project.application.service.finder;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.exception.ProjectMemberNotFoundException;
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
                .findByProjectIdAndMemberId(project.getId(), memberId)
                .orElseThrow(() ->
                        new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId));
    }

    public ProjectMember getActiveBy(Project project, Long memberId) {
        return queryRepository
                .findByProjectIdAndMemberIdAndSoftDeletedFalse(project.getId(), memberId)
                .orElseThrow(() ->
                        new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId));
    }

    public Set<Long> getExistingMemberIdsBy(Project project, Collection<Long> memberIds) {
        return queryRepository.findMemberIdsByProjectAndMemberIds(project, memberIds);
    }

    public boolean existsBy(Project project, Long memberId) {
        return queryRepository.existsByProjectAndMemberId(project, memberId);
    }
}
