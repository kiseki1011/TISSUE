package com.tissue.api.issue.service.command;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
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
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.exception.WorkspaceMemberNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommandService {

	private final IssueRepository issueRepository;
	private final WorkspaceRepository workspaceRepository;
	private final WorkspaceMemberRepository workspaceMemberRepository;
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
		Workspace workspace = findWorkspace(code);

		// Todo: Optional 사용을 고려
		Issue parentIssue = request.parentIssueKey() != null
			? findIssue(code, request.parentIssueKey()) : null;

		Issue issue = request.to(workspace, parentIssue);

		Issue savedIssue = issueRepository.save(issue);
		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateIssueResponse updateIssue(
		String code,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueRequest request
	) {
		Issue issue = findIssue(code, issueKey);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// Todo: Issue에서 검증 메서드 정의해서 사용하도록 리팩토링
		issueValidator.validateIssueTypeMatch(issue, request);

		request.update(issue);

		return UpdateIssueResponse.from(issue);
	}

	@Transactional
	public UpdateIssueStatusResponse updateIssueStatus(
		String code,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueStatusRequest request
	) {
		Issue issue = findIssue(code, issueKey);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		issue.updateStatus(request.status());

		return UpdateIssueStatusResponse.from(issue);
	}

	@Transactional
	public AssignParentIssueResponse assignParentIssue(
		String code,
		String issueKey,
		Long requesterWorkspaceMemberId,
		AssignParentIssueRequest request
	) {
		Issue issue = findIssue(code, issueKey);
		Issue parentIssue = findIssue(code, request.parentIssueKey());
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		issue.setParentIssue(parentIssue);

		return AssignParentIssueResponse.from(issue);
	}

	@Transactional
	public RemoveParentIssueResponse removeParentIssue(
		String code,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = findIssue(code, issueKey);

		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// Todo: Issue에 validateCanRemoveParentIssue 형태로 리팩토링
		// sub-task의 부모 제거 방지용 로직
		if (cannotRemoveParent(issue)) {
			throw new InvalidOperationException("Cannot remove the parent of this issue");
		}
		issue.removeParentRelationship();

		return RemoveParentIssueResponse.from(issue);
	}

	@Transactional
	public DeleteIssueResponse deleteIssue(
		String code,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = findIssue(code, issueKey);
		WorkspaceMember requester = findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// Todo: Issue에서 검증 메서드 정의해서 사용하도록 리팩토링
		issueValidator.validateNotParentOfSubTask(issue);

		Long issueId = issue.getId();

		issueRepository.delete(issue);

		return DeleteIssueResponse.from(issueId, issueKey, LocalDateTime.now());
	}

	private Workspace findWorkspace(String code) {
		return workspaceRepository.findByCode(code)
			.orElseThrow(() -> new WorkspaceNotFoundException(code));
	}

	private WorkspaceMember findWorkspaceMember(Long id) {
		return workspaceMemberRepository.findById(id)
			.orElseThrow(() -> new WorkspaceMemberNotFoundException(id));
	}

	private Issue findIssue(String code, String issueKey) {
		return issueRepository.findByIssueKeyAndWorkspaceCode(issueKey, code)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, code));
	}

	private boolean cannotRemoveParent(Issue issue) {
		return !issue.validateCanRemoveParent();
	}
}
