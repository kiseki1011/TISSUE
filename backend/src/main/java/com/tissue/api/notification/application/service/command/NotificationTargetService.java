package com.tissue.api.notification.application.service.command;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationTargetService {

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final IssueReader issueReader;

	/**
	 * Retrieve all members in the workspace as notification targets.
	 */
	public List<WorkspaceMember> getWorkspaceWideMemberTargets(String workspaceCode) {
		return workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode);
	}

	/**
	 * Retrieve the issue subscribers (e.g., author, assignee, reviewers, watchers)
	 * as notification targets.
	 */
	public List<WorkspaceMember> getIssueSubscriberTargets(String issueKey, String workspaceCode) {

		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		Set<Long> subscriberIds = issue.getSubscriberMemberIds();

		return workspaceMemberRepository.findAllByWorkspaceCodeAndMemberIdIn(workspaceCode, subscriberIds);
	}

	/**
	 * Retrieve the reviewers of the issue as notification targets.
	 */
	public List<WorkspaceMember> getIssueReviewerTargets(String issueKey, String workspaceCode) {

		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		Set<Long> reviewerIds = issue.getReviewerMemberIds();

		return workspaceMemberRepository.findAllByWorkspaceCodeAndMemberIdIn(workspaceCode, reviewerIds);
	}

	/**
	 * Retrieve workspace administrators and a specific member as notification targets.
	 */
	public Set<WorkspaceMember> getAdminAndSpecificMemberTargets(String workspaceCode, Long memberId) {

		Set<WorkspaceMember> targets = workspaceMemberRepository.findAdminsByWorkspaceCode(workspaceCode);

		workspaceMemberRepository.findByMemberIdAndWorkspaceCode(memberId, workspaceCode)
			.ifPresent(targets::add);

		return targets;
	}

	/**
	 * Retrieve a specific member as a notification target.
	 */
	public Set<WorkspaceMember> getSpecificMemberTarget(String workspaceCode, Long memberId) {

		Set<WorkspaceMember> target = new HashSet<>();

		workspaceMemberRepository.findByMemberIdAndWorkspaceCode(memberId, workspaceCode)
			.ifPresent(target::add);

		return target;
	}
}
