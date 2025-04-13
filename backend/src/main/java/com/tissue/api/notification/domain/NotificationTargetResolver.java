package com.tissue.api.notification.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.service.command.IssueReader;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;
import com.tissue.api.workspacemember.service.command.WorkspaceMemberReader;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationTargetResolver {

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final WorkspaceMemberReader workspaceMemberReader;
	private final IssueReader issueReader;

	/**
	 * 워크스페이스 전체 멤버를 알림 대상으로 결정
	 */
	public List<WorkspaceMember> getWorkspaceWideMemberTargets(String workspaceCode) {
		return workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode);
	}

	/**
	 * 이슈 구독자(작성자, 담당자, 리뷰어, 워처 등)를 알림 대상으로 결정
	 */
	public List<WorkspaceMember> getIssueSubscriberTargets(String issueKey, String workspaceCode) {

		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		Set<Long> subscriberIds = issue.getSubscriberIds();
		return workspaceMemberRepository.findAllByIdIn(subscriberIds);
	}

	/**
	 * 이슈의 리뷰어(검토자)들을 알림 대상으로 결정
	 */
	public List<WorkspaceMember> getIssueReviewerTargets(String issueKey, String workspaceCode) {

		Issue issue = issueReader.findIssue(issueKey, workspaceCode);

		Set<Long> reviewerIds = issue.getReviewerIds();
		return workspaceMemberRepository.findAllByIdIn(reviewerIds);
	}

	public Set<WorkspaceMember> getAdminsAndSpecificMember(String workspaceCode, Long workspaceMemberId) {
		Set<WorkspaceMember> targets = workspaceMemberRepository
			.findAdminsByWorkspaceCode(workspaceCode);

		workspaceMemberRepository.findByWorkspaceCodeAndId(workspaceCode, workspaceMemberId)
			.ifPresent(targets::add);

		return targets;
	}

	public Set<WorkspaceMember> getSpecificMember(String workspaceCode, Long workspaceMemberId) {

		Set<WorkspaceMember> target = new HashSet<>();

		workspaceMemberRepository.findByWorkspaceCodeAndId(workspaceCode, workspaceMemberId)
			.ifPresent(target::add);

		return target;
	}
}
