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
import com.tissue.api.issue.application.port.out.IssueFieldValueQueryRepository;
import com.tissue.api.issue.application.port.out.IssueQueryRepository;
import com.tissue.api.issue.application.port.out.IssueRelationQueryRepository;
import com.tissue.api.issue.application.port.out.IssueReviewerQueryRepository;
import com.tissue.api.issue.application.port.out.IssueSubscriberQueryRepository;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.IssueFieldValue;
import com.tissue.api.issue.domain.IssueRelation;
import com.tissue.api.issue.domain.IssueReviewer;
import com.tissue.api.issue.domain.IssueSubscriber;
import com.tissue.api.issue.exception.IssueNotFoundException;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workspacemember.application.finder.WorkspaceMemberQueryFinder;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class IssueQueryService implements IssueQueryUseCase {

	private final IssueQueryRepository issueQueryRepo;
	private final IssueFieldValueQueryRepository fieldValueQueryRepo;
	private final IssueSubscriberQueryRepository subscriberQueryRepo;
	private final IssueReviewerQueryRepository reviewerQueryRepo;
	private final IssueRelationQueryRepository relationQueryRepo;
	private final WorkspaceMemberQueryFinder wmFinder;

	@Override
	public IssueBasicInfo getBasic(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		WorkspaceMember author = wmFinder.findIncludingArchived(issue.getCreatedBy(), workspaceKey);
		WorkspaceMember updatedBy = wmFinder.findIncludingArchived(issue.getLastModifiedBy(), workspaceKey);

		return IssueBasicInfo.from(issue, author, updatedBy);
	}

	@Override
	public IssueCommonDetail getCommon(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithDetail(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		WorkspaceMember author = wmFinder.findIncludingArchived(issue.getCreatedBy(), workspaceKey);
		WorkspaceMember updatedBy = wmFinder.findIncludingArchived(issue.getLastModifiedBy(), workspaceKey);
		List<IssueReviewer> reviewers = reviewerQueryRepo.findByIssue(workspaceKey, issueKey);

		return IssueCommonDetail.from(issue, author, updatedBy, reviewers);
	}

	@Override
	public IssueCustomDetail getCustom(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		List<IssueFieldValue> fieldValues = fieldValueQueryRepo.findByIssue(workspaceKey, issueKey);

		return IssueCustomDetail.from(issue, fieldValues);
	}

	@Override
	public IssueIdentificationInfo getParent(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithParent(workspaceKey, issueKey)
			.orElseThrow(() -> new IssueNotFoundException(
				"Issue not found. workspaceKey: %s, issueKey: %s"
					.formatted(workspaceKey, issueKey)
			));

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

	// TODO: 이 방식 대신 Issue를 relations와 함께 join fetch해서 조회하고, issue.getRelations()를 통해 조회하도록 하는게 좋을까?
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

	// TODO: findWithBasicInfo 대신 아무런 join fetch를 사용하지 않는 find라는 기본 조회 메서드를 만들어서 사용할까?
	//  왜냐하면 여기서 join fetch를 사용할 이유가 없음
	@Override
	public ParticipantInfo getAuthor(String workspaceKey, String issueKey) {
		Issue issue = issueQueryRepo.findWithBasicInfo(workspaceKey, issueKey)
			.orElseThrow(() -> new RuntimeException("Issue not found"));

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
			.orElseThrow(() -> new RuntimeException("Issue not found"));

		Workflow workflow = issue.getIssueType().getWorkflow();

		return workflow.getTransitions().stream()
			.filter(t -> t.getSourceState().equals(issue.getCurrentState()))
			.map(TransitionDetail::from)
			.toList();
	}
}
