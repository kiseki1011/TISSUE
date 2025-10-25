package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.response.IssueDetailDto;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.presentation.dto.response.TransitionResponse;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService {

	private final IssueQueryRepository queryRepository;

	public IssueDetailDto getIssueDetails(String workspaceKey, String issueKey) {
		return queryRepository.findDetailedIssue(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));
	}

	/**
	 * 현재 상태에서 가능한 모든 Transition 조회
	 * <p>
	 * 반환되는 Transition:
	 * - Issue의 currentStatus를 source로 하는 Transition들
	 * - Guard 조건은 체크하지 않음 (실제 실행 시 체크)
	 */
	public List<TransitionResponse> getAvailableTransitions(String workspaceKey, String issueKey) {
		Issue issue = queryRepository.findIssue(issueKey, workspaceKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		Workflow workflow = issue.getIssueType().getWorkflow();

		// Workflow의 모든 Transition 중에서
		// 현재 상태(currentStatus)에서 출발하는 Transition만 필터링
		// 예: 현재 "IN_PROGRESS"면 "IN_PROGRESS -> X" 형태만 선택
		return workflow.getTransitions().stream()
			.filter(t -> t.getSourceState().equals(issue.getCurrentState()))
			.map(TransitionResponse::from)
			.toList();
	}
}
