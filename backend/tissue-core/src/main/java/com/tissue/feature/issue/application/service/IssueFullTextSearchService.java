package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.request.IssueSearchCondition;
import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.repository.IssueFullTextSearchRepository;
import com.tissue.feature.issue.application.port.usecase.IssueFullTextSearchUseCase;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.dto.ProjectIdentifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueFullTextSearchService implements IssueFullTextSearchUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueFullTextSearchRepository ftsRepository;
    private final IssueSearchPolicy policy;

    @Override
    public Page<IssueSummary> ftsByProject(
            ProjectIdentifier pid, IssueSearchCondition condition, Pageable pageable, Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        if (condition.keyword() == null || condition.keyword().isBlank()) {
            return Page.empty(pageable);
        }

        IssueSearchCondition resolved = policy.resolveCurrentSprint(condition, project);
        Pageable effective = policy.applyDefaultSort(pageable);

        return ftsRepository.ftsByProject(project, resolved, effective).map(IssueSummary::from);
    }
}
