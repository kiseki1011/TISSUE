package com.tissue.feature.issue.application.service;

import com.tissue.feature.issue.application.dto.response.IssueSummary;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issue.application.port.usecase.IssueTrashUseCase;
import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.shared.dto.PageSizes;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueTrashService implements IssueTrashUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueQueryRepository issueQueryRepository;

    @Override
    public Page<IssueSummary> listDeletedByProject(
            ProjectIdentifier pid, Set<Long> authorMemberIds, int page, int size, Long actorMemberId) {
        Project project = projectFinder.getByProjectKey(pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        Pageable pageable = PageSizes.clampedPageRequest(page, size);
        Page<Issue> deleted = authorMemberIds.isEmpty()
                ? issueQueryRepository.findDeletedByProjectId(project.getId(), pageable)
                : issueQueryRepository.findDeletedByProjectIdAndAuthors(project.getId(), authorMemberIds, pageable);

        // fromDeleted carries the body so the TUI can show the description read-only.
        return deleted.map(IssueSummary::fromDeleted);
    }
}
