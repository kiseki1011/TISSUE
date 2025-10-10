package com.tissue.api.issue.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.workspacemember.application.service.command.WorkspaceMemberFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

// TODO: IssueAssociateService라는 이름이 더 좋을까? 아니면 기존의 IssueCollaboratorService?
//  아니면 더 좋은 이름이 있으려나? (더 좋은 이름이 있다면 IssueCollaboratorController의 이름도 다 같이 변경 예정)
@Service
@RequiredArgsConstructor
public class IssueCollaboratorService {

	private final IssueFinder issueFinder;
	private final WorkspaceMemberFinder workspaceMemberFinder;

	// TODO: 아래의 메서드들의 변수명에 target, actor 등을 사용하고 있는데, 이렇게 지어도 괜찮나?
	//  내 생각은 target 이나 actor 같은 변수를 통해 의미를 나타내고 싶었음.
	//  그냥 workspaceMember로 두는게 좋을건가?

	@Transactional
	public IssueResponse addAssignee(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.addAssignee(target);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse removeAssignee(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeAssignee(target);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse subscribe(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember actor = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.addSubscriber(actor);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse cancelSubscription(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember actor = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeSubscriber(actor);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse addReviewer(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.addReviewer(target);

		return IssueResponse.from(issue);
	}

	@Transactional
	public IssueResponse removeReviewer(String workspaceKey, String issueKey, Long memberId) {
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);
		WorkspaceMember target = workspaceMemberFinder.findWorkspaceMember(memberId, workspaceKey);

		issue.removeReviewer(target);

		return IssueResponse.from(issue);
	}
}
