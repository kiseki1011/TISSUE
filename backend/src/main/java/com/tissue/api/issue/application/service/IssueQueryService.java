package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.response.IssueDetailResponse;
import com.tissue.api.issue.application.port.in.IssueQueryUseCase;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.presentation.dto.response.TransitionResponse;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService implements IssueQueryUseCase {

	private final IssueQueryRepository queryRepository;

	// TODO: findDetailedIssue에서 IssueDetailResponse으로 프로젝션 되도록 JPQL을 사용해서 구현했는데,
	//  그냥 Issue를 join fetch를 사용해서 지연로딩 없이 가져올 연관 엔티티들도 같이 로드하고 나서
	//  Issue -> IssueDetailResponse로 변환하는 방식을 사용할까?
	public IssueDetailResponse getIssueDetails(String workspaceKey, String issueKey) {
		return queryRepository.findDetailedIssue(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));
	}

	public List<TransitionResponse> getAvailableTransitions(String workspaceKey, String issueKey) {
		Issue issue = queryRepository.findIssue(issueKey, workspaceKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		Workflow workflow = issue.getIssueType().getWorkflow();

		return workflow.getTransitions().stream()
			.filter(t -> t.getSourceState().equals(issue.getCurrentState()))
			.map(TransitionResponse::from)
			.toList();
	}
}
