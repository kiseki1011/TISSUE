package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.issue.application.dto.response.IssueCommonDetail;
import com.tissue.api.issue.application.dto.response.IssueCustomDetail;
import com.tissue.api.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.api.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.api.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;
import com.tissue.api.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.api.issue.application.dto.response.info.IssueIdentificationInfo;
import com.tissue.api.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.api.issue.application.port.in.IssueQueryUseCase;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueFieldValue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.IssueReviewer;
import com.tissue.api.issue.domain.IssueSubscriber;
import com.tissue.api.issue.domain.port.out.IssueFieldValueQueryRepository;
import com.tissue.api.issue.domain.port.out.IssueQueryRepository;
import com.tissue.api.issue.domain.port.out.IssueRelationQueryRepository;
import com.tissue.api.issue.domain.port.out.IssueReviewerQueryRepository;
import com.tissue.api.issue.domain.port.out.IssueSubscriberQueryRepository;
import com.tissue.api.issue.domain.exception.IssueNotFoundException;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.api.workspace.domain.WorkspaceMember;

import lombok.RequiredArgsConstructor;

// TODO: project 애그리거트 추가 후 projectKey 관련 리팩토링
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService implements IssueQueryUseCase {

	private final IssueQueryRepository issueQueryRepo;
	private final IssueFieldValueQueryRepository issueFieldValueQueryRepo;
	private final IssueSubscriberQueryRepository subscriberQueryRepo;
	private final IssueReviewerQueryRepository reviewerQueryRepo;
	private final IssueRelationQueryRepository relationQueryRepo;
	private final WorkspaceMemberFinder wmFinder;

	@Override
	public IssueBasicInfo getBasic(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(workspaceKey, issueKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceKey));

		WorkspaceMember author = wmFinder.findIncludingArchived(issue.getCreatedBy(), workspaceKey);
		WorkspaceMember updatedBy = wmFinder.findIncludingArchived(issue.getLastModifiedBy(), workspaceKey);

		return IssueBasicInfo.from(issue, author, updatedBy);
	}

	@Override
	public IssueCommonDetail getCommon(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithDetail(workspaceKey, issueKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceKey));

		WorkspaceMember author = wmFinder.findIncludingArchived(issue.getCreatedBy(), workspaceKey);
		WorkspaceMember updatedBy = wmFinder.findIncludingArchived(issue.getLastModifiedBy(), workspaceKey);
		List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);

		return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
	}

	@Override
	public IssueCustomDetail getCustom(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(workspaceKey, issueKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceKey));

		List<IssueFieldValue> fieldValues = issueFieldValueQueryRepo.findByWorkspaceKeyAndIssueKey(
			workspaceKey,
			issueKey
		);

		return IssueCustomDetail.from(issue, fieldValues);
	}

	@Override
	public IssueIdentificationInfo getParent(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithParent(workspaceKey, issueKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceKey));

		Issue parent = issue.getParentIssue();
		if (parent == null) {
			return IssueIdentificationInfo.asNull();
		}

		return IssueIdentificationInfo.from(parent);
	}

	@Override
	public List<IssueIdentificationInfo> getChildren(String workspaceKey, String issueKey) {
		List<Issue> children = issueQueryRepo.findChildren(workspaceKey, issueKey);

		return children.stream()
			.map(IssueIdentificationInfo::from)
			.toList();
	}

	@Override
	public IssueRelationsDetail getRelations(String workspaceKey, String issueKey) {
		List<IssueRelation> allRelations = relationQueryRepo.findAllRelations(workspaceKey, issueKey);

		List<IssueRelation> outgoing = allRelations.stream()
			.filter(r -> r.getSourceIssue().getKey().equals(issueKey))
			.toList();

		List<IssueRelation> incoming = allRelations.stream()
			.filter(r -> r.getTargetIssue().getKey().equals(issueKey))
			.toList();

		return IssueRelationsDetail.from(outgoing, incoming);
	}

	@Override
	public ParticipantInfo getAuthor(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(workspaceKey, issueKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceKey));

		WorkspaceMember author = wmFinder.findIncludingArchived(issue.getCreatedBy(), workspaceKey);

		return ParticipantInfo.from(author);
	}

	@Override
	public IssueReviewersDetail getReviewers(String workspaceKey, String issueKey) {
		List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);
		return IssueReviewersDetail.from(reviewers);
	}

	@Override
	public IssueSubscribersDetail getSubscribers(String workspaceKey, String issueKey) {
		List<IssueSubscriber> subscribers = subscriberQueryRepo.findByIssue(workspaceKey, issueKey);
		return IssueSubscribersDetail.from(subscribers);
	}

	@Override
	public List<TransitionDetail> getAvailableTransitions(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(issueKey, workspaceKey)
			.orElseThrow(() -> new IssueNotFoundException(issueKey, "projectKey", workspaceKey));

		Workflow workflow = issue.getIssueType().getWorkflow();

		return workflow.getTransitions().stream()
			.filter(t -> t.getSourceState().equals(issue.getCurrentState()))
			.map(TransitionDetail::from)
			.toList();
	}
}
