package com.tissue.api.sprint.application.service.query;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SprintQueryService {

	// private final SprintQueryRepository sprintQueryRepository;
	//
	// @Transactional(readOnly = true)
	// public SprintDetail getSprintDetail(
	// 	String workspaceCode,
	// 	String sprintKey
	// ) {
	// 	Sprint sprint = sprintQueryRepository.findBySprintKeyAndWorkspaceKeyWithIssues(sprintKey, workspaceCode)
	// 		.orElseThrow(() -> new ResourceNotFoundException(
	// 			String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
	// 				sprintKey, workspaceCode))
	// 		);
	//
	// 	return SprintDetail.from(sprint);
	// }

	// @Transactional(readOnly = true)
	// public Page<SprintIssueDetail> getSprintIssues(
	// 	String workspaceCode,
	// 	String sprintKey,
	// 	SprintIssueSearchCondition searchCondition,
	// 	Pageable pageable
	// ) {
	// 	// Todo
	// 	//  - existsBy로 바꾸자
	// 	//  - 굳이 스프린트의 유효성을 검증해야 하나? 어차피 클라이언트에서 제대로 된 sprintKey를 보낸다고 가정하면 안되나?
	// 	//  - workspaceCode는 어차피 컨트롤러단에서 유효성이 입증됨
	// 	// sprintRepository.findBySprintKeyAndWorkspaceCode(sprintKey, workspaceKey)
	// 	// 	.orElseThrow(() -> new ResourceNotFoundException(
	// 	// 		String.format("Sprint was not found with sprint key(%s) and workspace code(%s)",
	// 	// 			sprintKey, workspaceKey))
	// 	// 	);
	//
	// 	return sprintQueryRepository.findIssuesInSprint(sprintKey, workspaceCode, searchCondition, pageable)
	// 		.map(SprintIssueDetail::from);
	// }

	// @Transactional(readOnly = true)
	// public Page<SprintDetail> getSprints(
	// 	String workspaceCode,
	// 	SprintSearchCondition searchCondition,
	// 	Pageable pageable
	// ) {
	// 	return sprintQueryRepository.findSprintPageByWorkspaceKey(workspaceCode, searchCondition, pageable)
	// 		.map(SprintDetail::from);
	// }
}
