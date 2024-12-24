package com.tissue.api.issue.service.command;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.issue.presentation.dto.request.UpdateStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.UpdateStatusResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.WorkspaceRepository;
import com.tissue.api.workspace.exception.WorkspaceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class IssueCommandService {

	private final IssueRepository issueRepository;
	private final WorkspaceRepository workspaceRepository;

	/*
	 * Todo
	 *  - 생각해보면, 이미 RoleRequired에 대한 인터셉터에서 Workspace를 검증하는데 여기서 또 검증이 필요한가?
	 *    - RequestContextHolder에 대해 찾아보기
	 *  - 동시성 문제 해결을 위해서 이슈 생성에 spring-retry 적용
	 *    - 반복문, try-catch문 적용으로 issueKey의 유일성 제약 위반 예외 잡아서 처리하는 방법보다 나은 듯
	 *  - Workspace에서 issueKeyPrefix와 nextIssueNumber를 관리하기 때문에, Workspace에 Optimistic locking을 적용한다
	 */
	@Transactional
	public CreateIssueResponse createIssue(
		String code,
		CreateIssueRequest request
	) {
		Workspace workspace = workspaceRepository.findByCode(code)
			.orElseThrow(WorkspaceNotFoundException::new);

		Issue issue = request.to(
			workspace,
			findParentIssue(request.parentIssueId(), code).orElse(null)
		);

		Issue savedIssue = issueRepository.save(issue);
		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateStatusResponse updateIssueStatus(
		Long issueId,
		String code,
		UpdateStatusRequest request
	) {
		Issue issue = issueRepository.findByIdAndWorkspaceCode(issueId, code)
			.orElseThrow(IssueNotFoundException::new);

		issue.updateStatus(request.status());

		return UpdateStatusResponse.from(issue);
	}

	private Optional<Issue> findParentIssue(Long parentIssueId, String workspaceCode) {
		return Optional.ofNullable(parentIssueId)
			.map(id -> issueRepository.findByIdAndWorkspaceCode(id, workspaceCode)
				.orElseThrow(() -> new IssueNotFoundException("Issue does not exist in this workspace.")));
	}
}
