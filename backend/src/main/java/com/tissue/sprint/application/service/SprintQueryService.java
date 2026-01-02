package com.tissue.sprint.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.security.application.port.out.CurrentMemberProvider;
import com.tissue.sprint.application.dto.response.SprintDetail;
import com.tissue.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.sprint.application.port.in.SprintQueryUseCase;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.exception.SprintExceptions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SprintQueryService implements SprintQueryUseCase {

    private final IssueQueryRepository issueQueryRepository;
    private final SprintQueryRepository sprintQueryRepository;
    private final CurrentMemberProvider currentMemberProvider;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public SprintDetail getSprintDetail(String workspaceKey, String projectKey, Long sprintId) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.isViewer(workspaceKey, projectKey, actorMemberId);

        Sprint sprint = sprintQueryRepository
                .findByIdAndProject_Key(sprintId, projectKey)
                .orElseThrow(() -> SprintExceptions.notFound(sprintId, projectKey));

        return SprintDetail.from(sprint);
    }

    @Override
    public SprintIssueKeys getSprintIssueKeys(String workspaceKey, String projectKey, Long sprintId) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        projectAuthService.isViewer(workspaceKey, projectKey, actorMemberId);

        Sprint sprint = sprintQueryRepository
                .findByIdAndProject_Key(sprintId, projectKey)
                .orElseThrow(() -> SprintExceptions.notFound(sprintId, projectKey));

        List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);

        return new SprintIssueKeys(issueKeys);
    }
}
