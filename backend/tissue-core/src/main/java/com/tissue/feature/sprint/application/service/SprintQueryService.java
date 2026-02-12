package com.tissue.feature.sprint.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.port.repository.SprintQueryRepository;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.domain.Sprint;
import com.tissue.feature.sprint.domain.exception.SprintNotFoundException;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintQueryService implements SprintQueryUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueQueryRepository issueQueryRepository;
    private final SprintQueryRepository sprintQueryRepository;

    @Override
    public SprintDetail getSprintDetail(ProjectIdentifier projectIdentifier, Long sprintId, Long memberId) {

        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());
        projectMemberFinder.getBy(project, memberId);

        Sprint sprint = sprintQueryRepository
                .findByProject_KeyAndId(projectIdentifier.projectKey(), sprintId)
                .orElseThrow(() -> new SprintNotFoundException(projectIdentifier.projectKey(), sprintId));

        return SprintDetail.from(sprint);
    }

    @Override
    public SprintIssueKeys getSprintIssueKeys(ProjectIdentifier projectIdentifier, Long sprintId, Long memberId) {

        Project project = projectFinder.getBy(projectIdentifier.workspaceKey(), projectIdentifier.projectKey());
        projectMemberFinder.getBy(project, memberId);

        Sprint sprint = sprintQueryRepository
                .findByProject_KeyAndId(projectIdentifier.projectKey(), sprintId)
                .orElseThrow(() -> new SprintNotFoundException(projectIdentifier.projectKey(), sprintId));

        List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);

        return new SprintIssueKeys(issueKeys);
    }
}
