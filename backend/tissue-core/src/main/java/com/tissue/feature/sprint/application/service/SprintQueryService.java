package com.tissue.feature.sprint.application.service;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.domain.Sprint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintQueryService implements SprintQueryUseCase {

    private final SprintFinder sprintFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final IssueQueryRepository issueQueryRepository;

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
}
