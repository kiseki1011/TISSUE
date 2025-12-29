package com.tissue.sprint.application.service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.sprint.application.dto.request.GetSprintDetailQuery;
import com.tissue.sprint.application.dto.request.GetSprintIssueKeysQuery;
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

    @Override
    public SprintDetail getSprintDetail(GetSprintDetailQuery query) {
        Sprint sprint = sprintQueryRepository
                .findByIdAndProject_Key(query.sprintId(), query.projectKey())
                .orElseThrow(() -> SprintExceptions.notFound(query.sprintId(), query.projectKey()));

        return SprintDetail.from(sprint);
    }

    @Override
    public SprintIssueKeys getSprintIssueKeys(GetSprintIssueKeysQuery query) {
        Sprint sprint = sprintQueryRepository
                .findByIdAndProject_Key(query.sprintId(), query.projectKey())
                .orElseThrow(() -> SprintExceptions.notFound(query.sprintId(), query.projectKey()));

        List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);

        return new SprintIssueKeys(issueKeys);
    }
}
