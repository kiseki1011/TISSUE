package com.tissue.sprint.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.dto.ProjectMemberContext;
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

    @Override
    public SprintDetail getSprintDetail(Long sprintId, ProjectMemberContext actorContext) {
        Sprint sprint = sprintQueryRepository
                .findByProject_KeyAndId(actorContext.projectKey(), sprintId)
                .orElseThrow(() -> new SprintNotFoundException(actorContext.projectKey(), sprintId));

        return SprintDetail.from(sprint);
    }

    @Override
    public SprintIssueKeys getSprintIssueKeys(Long sprintId, ProjectMemberContext actorContext) {
        Sprint sprint = sprintQueryRepository
                .findByProject_KeyAndId(actorContext.projectKey(), sprintId)
                .orElseThrow(() -> new SprintNotFoundException(actorContext.projectKey(), sprintId));

        List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);

        return new SprintIssueKeys(issueKeys);
    }
}
