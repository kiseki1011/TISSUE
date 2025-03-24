package com.tissue.api.issue.service.command;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.event.IssueCreatedEvent;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.issue.presentation.dto.request.AssignParentIssueRequest;
import com.tissue.api.issue.presentation.dto.request.UpdateIssueStatusRequest;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.AssignParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.RemoveParentIssueResponse;
import com.tissue.api.issue.presentation.dto.response.UpdateIssueStatusResponse;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateIssueResponse;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.service.command.WorkspaceReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class IssueCommandService {

	private final IssueReader issueReader;
	private final WorkspaceReader workspaceReader;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueRepository issueRepository;

	private final ApplicationEventPublisher eventPublisher;

	// private final IssueEventPublisher eventPublisher;

	@Transactional
	public CreateIssueResponse createIssue(
		String workspaceCode,
		Long currentWorkspaceMemberId,
		CreateIssueRequest request
	) {
		Workspace workspace = workspaceReader.findWorkspace(workspaceCode);

		Issue issue = request.toIssue(workspace);
		Issue savedIssue = issueRepository.save(issue);

		eventPublisher.publishEvent(new IssueCreatedEvent(
			issue.getId(),
			issue.getIssueKey(),
			workspaceCode,
			currentWorkspaceMemberId
		));

		return CreateIssueResponse.from(savedIssue);
	}

	@Transactional
	public UpdateIssueResponse updateIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		issue.validateIssueTypeMatch(request.getType());

		// Todo: AuthorizationService를 만들어서 권한 검사 로직 분리
		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// 스토리 포인트 변경 감지를 위해 이전 값 저장
		Integer oldStoryPoint = issue.getStoryPoint();

		request.updateNonNullFields(issue);

		// if (request.hasStoryPointValue()) {
		// 	eventPublisher.publishStoryPointChanged(
		// 		issue,
		// 		oldStoryPoint,
		// 		issue.getStoryPoint(),
		// 		requesterWorkspaceMemberId
		// 	);
		// }
		// Todo: publishStoryPointChanged를 호출하는 로직이 너무 이상함.
		//  일단 publishIssueUpdated를 통해 이슈를 발행하고, 처리는 리스너에서 하도록 구현하는 것이 맞음
		// eventPublisher.publishIssueUpdated(issue, creatorWorkspaceMemberId);

		return UpdateIssueResponse.from(issue);
	}

	@Transactional
	public UpdateIssueStatusResponse updateIssueStatus(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		UpdateIssueStatusRequest request
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// 상태 변경 전 이전 상태 저장
		IssueStatus oldStatus = issue.getStatus();

		issue.updateStatus(request.status());

		// 상태 변경 이벤트 발행
		// eventPublisher.publishStatusChanged(
		// 	issue,
		// 	oldStatus,
		// 	request.status(),
		// 	requesterWorkspaceMemberId
		// );

		return UpdateIssueStatusResponse.from(issue);
	}

	@Transactional
	public AssignParentIssueResponse assignParentIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId,
		AssignParentIssueRequest request
	) {
		Issue childIssue = issueReader.findIssue(issueKey, workspaceCode);
		Issue parentIssue = issueReader.findIssue(request.parentIssueKey(), workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			childIssue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// 이전 부모 저장
		Issue oldParent = childIssue.getParentIssue();

		childIssue.updateParentIssue(parentIssue);

		// 부모 변경 이벤트 발행
		// eventPublisher.publishParentChanged(
		// 	childIssue,
		// 	oldParent,
		// 	parentIssue,
		// 	requesterWorkspaceMemberId
		// );

		return AssignParentIssueResponse.from(childIssue);
	}

	@Transactional
	public RemoveParentIssueResponse removeParentIssue(
		String workspaceCode,
		String issueKey,
		Long requesterWorkspaceMemberId
	) {
		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		WorkspaceMember requester = workspaceMemberReader.findWorkspaceMember(requesterWorkspaceMemberId);

		if (requester.roleIsLowerThan(WorkspaceRole.MANAGER)) {
			issue.validateIsAssigneeOrAuthor(requesterWorkspaceMemberId);
		}

		// 이전 부모 저장
		Issue oldParent = issue.getParentIssue();

		// sub-task의 부모 제거 방지용 로직
		issue.validateCanRemoveParent();
		issue.removeParentRelationship();

		// 부모 변경 이벤트 발행
		// eventPublisher.publishParentChanged(
		// 	issue,
		// 	oldParent,
		// 	null,
		// 	requesterWorkspaceMemberId
		// );

		return RemoveParentIssueResponse.from(issue);
	}
}
