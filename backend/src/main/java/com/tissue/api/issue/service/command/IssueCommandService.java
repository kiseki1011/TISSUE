package com.tissue.api.issue.service.command;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.exception.CannotRemoveParentException;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.UpdateIssueStatusResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.presentation.dto.response.delete.DeleteIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateIssueResponse;
import com.tissue.api.issue.validator.IssueValidator;
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
	private final IssueValidator issueValidator;

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
			findParentIssue(request.parentIssueKey(), code).orElse(null)
		);

		Issue savedIssue = issueRepository.save(issue);
		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateIssueResponse updateIssue(
		String code,
		String issueKey,
		UpdateIssueRequest request
	) {
		Issue issue = findIssue(code, issueKey);

		issueValidator.validateIssueTypeMatch(issue, request);

		request.update(issue);

		return UpdateIssueResponse.from(issue);
	}

	@Transactional
	public UpdateIssueStatusResponse updateIssueStatus(
		String code,
		String issueKey,
		UpdateIssueStatusRequest request
	) {
		Issue issue = findIssue(code, issueKey);

		issue.updateStatus(request.status());

		return UpdateIssueStatusResponse.from(issue);
	}

	@Transactional
	public AssignParentIssueResponse assignParentIssue(
		String code,
		String issueKey,
		AssignParentIssueRequest request
	) {
		Issue issue = findIssue(code, issueKey);
		Issue parentIssue = findIssue(code, request.parentIssueKey());

		issue.setParentIssue(parentIssue);

		return AssignParentIssueResponse.from(issue);
	}

	@Transactional
	public RemoveParentIssueResponse removeParentIssue(
		String code,
		String issueKey
	) {
		Issue issue = findIssue(code, issueKey);

		if (cannotRemoveParent(issue)) {
			throw new CannotRemoveParentException();
		}
		issue.removeParentRelationship();

		return RemoveParentIssueResponse.from(issue);
	}

	@Transactional
	public DeleteIssueResponse deleteIssue(
		String code,
		String issueKey
	) {
		Issue issue = findIssue(code, issueKey);

		issueValidator.validateNotParentOfSubTask(issue);

		Long issueId = issue.getId();

		issueRepository.delete(issue);

		return DeleteIssueResponse.from(issueId, issueKey, LocalDateTime.now());
	}

	/**
	 * Todo
	 *  - 기존에는 issueId로 찾던것을 issueKey로 찾도록 변경했음
	 *  - 로직을 변경해야 함 -> 왜냐하면 code + issueKey로 찾기 때문에 해당 워크스페이스에 존재할 수 밖에 없음
	 *  - 기존 id로 찾는 방식은 다른 워크스페이스에 존재하는 이슈를 조회가 가능했기 때문에 아래 처럼 사용
	 *  - 그러나 지금은 그럴 필요가 없고 그냥 findIssue()를 사용하면 될 듯
	 */
	private Optional<Issue> findParentIssue(String parentIssueKey, String workspaceCode) {
		return Optional.ofNullable(parentIssueKey)
			.map(issueKey -> issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, workspaceCode)
				.orElseThrow(() -> new IssueNotFoundException("Issue does not exist in this workspace.")));
	}

	private Issue findIssue(String code, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(IssueNotFoundException::new);
	}

	private boolean cannotRemoveParent(Issue issue) {
		return !issue.canRemoveParentRelationship();
	}
}
