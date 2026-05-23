package com.tissue.feature.sprint.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.dto.response.SprintSummary;
import com.tissue.feature.sprint.application.port.repository.SprintQueryRepository;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintQueryService implements SprintQueryUseCase {

    private final SprintFinder sprintFinder;
    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueQueryRepository issueQueryRepository;
    private final SprintQueryRepository sprintQueryRepository;

    @Override
    public SprintDetail getSprintDetail(String workspaceKey, Long sprintId, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);
        projectMemberFinder.getBy(sprint.getProject(), actorMemberId);

        return SprintDetail.from(sprint);
    }

    @Override
    public SprintIssueKeys getSprintIssueKeys(String workspaceKey, Long sprintId, Long actorMemberId) {
        Sprint sprint = sprintFinder.getWithProject(workspaceKey, sprintId);
        projectMemberFinder.getBy(sprint.getProject(), actorMemberId);

        List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);

        return new SprintIssueKeys(issueKeys);
    }

    @Override
    public Page<SprintSummary> getProjectSprints(
            ProjectIdentifier pid, @Nullable Set<SprintStatus> statuses, Pageable pageable, Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        var page = (statuses == null || statuses.isEmpty())
                ? sprintQueryRepository.findAllByProject(project, pageable)
                : sprintQueryRepository.findAllByProjectAndStatusIn(project, statuses, pageable);
        return page.map(SprintSummary::from);
    }
}
