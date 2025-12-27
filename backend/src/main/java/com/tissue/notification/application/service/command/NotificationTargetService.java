package com.tissue.notification.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tissue.issue.application.service.finder.IssueFinder;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

// TODO: NotificationTargetResolver가 더 좋은 표현일듯
@Service
@RequiredArgsConstructor
public class NotificationTargetService {

	private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;
	private final IssueFinder issueFinder;

	/**
	 * Retrieve all members in the workspace as notification targets.
	 */
	public List<WorkspaceMember> getWorkspaceWideMemberTargets(String workspaceCode) {
		return workspaceMemberQueryRepository.findAllByWorkspace_Key(workspaceCode);
	}

	/**
	 * Retrieve the issue associates (e.g., author, assignee, reviewers, subcribers)
	 * as notification targets.
	 */
	// public List<WorkspaceMember> getIssueAssociatesTargets(String issueKey, String workspaceCode) {
	//
	// 	Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
	//
	//  // author, assignees, reviewers, subcribers, 등... 전부 가져오기
	//
	// 	return workspaceMemberRepository.findAllByWorkspace_KeyAndMember_IdIn(workspaceCode, associateIds);
	// }

	/**
	 * Retrieve the reviewers of the issue as notification targets.
	 */
	// public List<WorkspaceMember> getIssueReviewerTargets(String issueKey, String workspaceCode) {
	//
	// 	Issue issue = issueFinder.findIssue(issueKey, workspaceCode);
	// 	Set<Long> reviewerIds = issue.getReviewerMemberIds();
	//
	// 	return workspaceMemberRepository.findAllByWorkspace_KeyAndMember_IdIn(workspaceCode, reviewerIds);
	// }

	/**
	 * Retrieve workspace administrators and a specific member as notification targets.
	 */
	public Set<WorkspaceMember> getAdminAndSpecificMemberTargets(String workspaceCode, Long memberId) {

		Set<WorkspaceMember> targets = workspaceMemberQueryRepository.findAdminsByWorkspace_Key(
			workspaceCode,
			Set.of(WorkspaceRole.ADMIN, WorkspaceRole.OWNER)
		);

		workspaceMemberQueryRepository.findByMember_IdAndWorkspaceKey(memberId, workspaceCode)
			.ifPresent(targets::add);

		return targets;
	}

	/**
	 * Retrieve a specific member as a notification target.
	 */
	public Set<WorkspaceMember> getSpecificMemberTarget(String workspaceCode, Long memberId) {

		Set<WorkspaceMember> target = new HashSet<>();

		workspaceMemberQueryRepository.findByMember_IdAndWorkspaceKey(memberId, workspaceCode)
			.ifPresent(target::add);

		return target;
	}
}
