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

    // TODO: use JOIN FETCH(or some other way) with WorkspaceMember at findAnyByProjectIdAndMemberId
    //  for optimization
    // TODO: find -> get
    public ProjectMember findBy(Project project, Long memberId) {
        return queryRepository
                .findAnyByProjectIdAndMemberId(project.getId(), memberId)
                .orElseThrow(() ->
                        new ProjectMemberNotFoundException(project.getWorkspaceKey(), project.getKey(), memberId));
    }

    // TODO: getIncludingSoftDeleted
    //  - is there a better name?
    //  - a pagination api
    //  - is used by ADMIN(WorkspaceRole, ProjectRole, SystemRole) to see all ProjectMember for a project
    //  including soft-deleted ProjectMembers

    // TODO: find -> get
    public Set<Long> findExistingMemberIdsBy(Project project, Collection<Long> memberIds) {
        return queryRepository.findMemberIdsByProjectAndMemberIds(project, memberIds);
    }

    public boolean existsBy(Project project, Long memberId) {
        return queryRepository.existsByProjectAndMemberId(project, memberId);
    }
}
