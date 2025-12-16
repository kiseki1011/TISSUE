package com.tissue.sprint.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.sprint.application.dto.request.GetSprintDetailQuery;
import com.tissue.sprint.application.dto.request.GetSprintIssueKeysQuery;
import com.tissue.sprint.application.dto.response.SprintDetail;
import com.tissue.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.sprint.application.port.in.SprintQueryUseCase;
import com.tissue.sprint.application.service.finder.SprintFinder;
import com.tissue.sprint.domain.Sprint;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintQueryService implements SprintQueryUseCase {

	private final SprintFinder sprintFinder;
	private final IssueQueryRepository issueQueryRepository;

	@Override
	public SprintDetail getSprintDetail(GetSprintDetailQuery query) {

		Sprint sprint = sprintFinder.findBy(query.sprintId(), query.projectKey(), query.workspaceKey());
		return SprintDetail.from(sprint);
	}

	@Override
	public SprintIssueKeys getSprintIssueKeys(GetSprintIssueKeysQuery query) {

		Sprint sprint = sprintFinder.findBy(query.sprintId(), query.projectKey(), query.workspaceKey());
		List<String> issueKeys = issueQueryRepository.findIssueKeysBySprint(sprint);
		return new SprintIssueKeys(issueKeys);
	}
}
