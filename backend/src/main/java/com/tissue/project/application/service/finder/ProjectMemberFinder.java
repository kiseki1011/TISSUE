package com.tissue.project.application.service.finder;

import com.tissue.project.application.port.out.ProjectMemberQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.ProjectMember;
import com.tissue.project.domain.exception.ProjectExceptions;
import java.util.Collection;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMemberFinder {

    private final ProjectMemberQueryRepository queryRepository;

    // TODO: use JOIN FETCH(or some other way) with WorkspaceMember at findAnyByProjectIdAndMemberId
    // for optimization
    public ProjectMember findBy(Project project, Long memberId) {
        return queryRepository
                .findAnyByProjectIdAndMemberId(project.getId(), memberId)
                .orElseThrow(() -> ProjectExceptions.memberNotFound(project, memberId));
    }

    public Set<Long> findExistingMemberIdsBy(Project project, Collection<Long> memberIds) {
        return queryRepository.findMemberIdsByProjectAndMemberIds(project, memberIds);
    }

    public boolean existsBy(Project project, Long memberId) {
        return queryRepository.existsByProjectAndMemberId(project, memberId);
    }
}
