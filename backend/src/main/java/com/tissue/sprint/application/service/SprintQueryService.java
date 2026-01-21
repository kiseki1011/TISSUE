package com.tissue.sprint.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.sprint.application.dto.response.SprintDetail;
import com.tissue.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.sprint.application.port.in.SprintQueryUseCase;
import com.tissue.sprint.application.port.out.SprintQueryRepository;
import com.tissue.sprint.domain.Sprint;
import com.tissue.sprint.domain.exception.SprintNotFoundException;
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
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public SprintDetail getSprintDetail(Long sprintId, ProjectMemberContext actorContext) {
        projectAuthService.requireProjectViewer(actorContext);

        Sprint sprint = sprintQueryRepository
                .findByIdAndProject_Key(sprintId, actorContext.projectKey())
                .orElseThrow(() -> new SprintNotFoundException(sprintId, actorContext.projectKey()));

        return SprintDetail.from(sprint);
    }

    @Override
    public SprintIssueKeys getSprintIssueKeys(Long sprintId, ProjectMemberContext actorContext) {
        projectAuthService.requireProjectViewer(actorContext);

        Sprint sprint = sprintQueryRepository
                .findByIdAndProject_Key(sprintId, actorContext.projectKey())
                .orElseThrow(() -> new SprintNotFoundException(sprintId, actorContext.projectKey()));

        List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);

        return new SprintIssueKeys(issueKeys);
    }
}
