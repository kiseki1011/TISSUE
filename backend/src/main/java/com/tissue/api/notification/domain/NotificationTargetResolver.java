package com.tissue.api.notification.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.service.reader.IssueReader;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.infrastructure.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationTargetResolver {

	private final WorkspaceMemberRepository workspaceMemberRepository;
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
		Set<Long> subscriberIds = issue.getSubscriberMemberIds();

		return workspaceMemberRepository.findAllByWorkspaceCodeAndMemberIdIn(workspaceCode, subscriberIds);
	}

	/**
	 * 이슈의 리뷰어(검토자)들을 알림 대상으로 결정
	 */
	public List<WorkspaceMember> getIssueReviewerTargets(String issueKey, String workspaceCode) {

		Issue issue = issueReader.findIssue(issueKey, workspaceCode);
		Set<Long> reviewerIds = issue.getReviewerMemberIds();

		return workspaceMemberRepository.findAllByWorkspaceCodeAndMemberIdIn(workspaceCode, reviewerIds);
	}

	public Set<WorkspaceMember> getAdminsAndSpecificMember(String workspaceCode, Long memberId) {
		Set<WorkspaceMember> targets = workspaceMemberRepository
			.findAdminsByWorkspaceCode(workspaceCode);

		workspaceMemberRepository.findByMemberIdAndWorkspaceCode(memberId, workspaceCode)
			.ifPresent(targets::add);

		return targets;
	}

	public Set<WorkspaceMember> getSpecificMember(String workspaceCode, Long memberId) {

		Set<WorkspaceMember> target = new HashSet<>();

		workspaceMemberRepository.findByMemberIdAndWorkspaceCode(memberId, workspaceCode)
			.ifPresent(target::add);

		return target;
	}
}
