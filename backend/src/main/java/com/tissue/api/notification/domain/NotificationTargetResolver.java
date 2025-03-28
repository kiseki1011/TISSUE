package com.tissue.api.notification.domain;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.common.exception.type.ResourceNotFoundException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.repository.IssueRepository;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NotificationTargetResolver {

	private final WorkspaceMemberRepository workspaceMemberRepository;
	private final IssueRepository issueRepository;

	/**
	 * 워크스페이스 전체 멤버를 알림 대상으로 결정
	 */
	public List<WorkspaceMember> getWorkspaceWideMemberTargets(String workspaceCode) {
		return workspaceMemberRepository.findAllByWorkspaceCode(workspaceCode);
	}

	/**
	 * 이슈 구독자(작성자, 담당자, 리뷰어, 워처 등)를 알림 대상으로 결정
	 */
	public List<WorkspaceMember> getIssueSubscriberTargets(Long issueId) {
		Issue issue = issueRepository.findById(issueId)
			.orElseThrow(() -> new ResourceNotFoundException("Issue not found: " + issueId));

		Set<Long> subscriberIds = issue.getSubscriberIds();
		return workspaceMemberRepository.findAllByIdIn(subscriberIds);
	}
}
