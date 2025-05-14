package com.tissue.api.sprint.application.service.query;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.sprint.domain.model.Sprint;
import com.tissue.api.sprint.infrastructure.repository.SprintQueryRepository;
import com.tissue.api.sprint.presentation.condition.SprintIssueSearchCondition;
import com.tissue.api.sprint.presentation.condition.SprintSearchCondition;
import com.tissue.api.sprint.presentation.dto.response.SprintDetail;
import com.tissue.api.sprint.presentation.dto.response.SprintIssueDetail;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintQueryService {

	private final SprintQueryRepository sprintQueryRepository;

	@Transactional(readOnly = true)
	public SprintDetail getSprintDetail(
		String workspaceCode,
		String sprintKey
	) {
		Sprint sprint = sprintQueryRepository.findBySprintKeyAndWorkspaceCodeWithIssues(sprintKey, workspaceCode)
			.orElseThrow(() -> new ResourceNotFoundException(
				String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
					sprintKey, workspaceCode))
			);

		return SprintDetail.from(sprint);
	}

	@Transactional(readOnly = true)
	public Page<SprintIssueDetail> getSprintIssues(
		String workspaceCode,
		String sprintKey,
		SprintIssueSearchCondition searchCondition,
		Pageable pageable
	) {
		// Todo
		//  - existsBy로 바꾸자
		//  - 굳이 스프린트의 유효성을 검증해야 하나? 어차피 클라이언트에서 제대로 된 sprintKey를 보낸다고 가정하면 안되나?
		//  - workspaceCode는 어차피 컨트롤러단에서 유효성이 입증됨
		// sprintRepository.findBySprintKeyAndWorkspaceCode(sprintKey, workspaceCode)
		// 	.orElseThrow(() -> new ResourceNotFoundException(
		// 		String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
		// 			sprintKey, workspaceCode))
		// 	);

		return sprintQueryRepository.findIssuesInSprint(sprintKey, workspaceCode, searchCondition, pageable)
			.map(SprintIssueDetail::from);
	}

	@Transactional(readOnly = true)
	public Page<SprintDetail> getSprints(
		String workspaceCode,
		SprintSearchCondition searchCondition,
		Pageable pageable
	) {
		return sprintQueryRepository.findSprintPageByWorkspaceCode(workspaceCode, searchCondition, pageable)
			.map(SprintDetail::from);
	}
}
