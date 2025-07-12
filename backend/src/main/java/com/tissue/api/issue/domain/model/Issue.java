package com.tissue.api.issue.domain.model;

import static com.tissue.api.issue.domain.model.enums.IssueStatus.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.model.enums.IssuePriority;
import com.tissue.api.issue.domain.model.enums.IssueRelationType;
import com.tissue.api.issue.domain.model.enums.IssueStatus;
import com.tissue.api.issue.domain.model.enums.IssueType;
import com.tissue.api.review.domain.model.enums.ReviewStatus;
import com.tissue.api.sprint.domain.model.SprintIssue;
import com.tissue.api.workspace.domain.model.Workspace;
import com.tissue.api.workspacemember.domain.model.WorkspaceMember;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@EqualsAndHashCode(of = {"issueKey", "workspaceCode"}, callSuper = false)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Issue extends BaseEntity {

	private static final int MAX_REVIEWERS = 10;
	private static final int MAX_ASSIGNEES = 50;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String issueKey;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", insertable = false, updatable = false)
	private IssueType type;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@Lob
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;

	private LocalDateTime startedAt;
	private LocalDateTime resolvedAt;
	private LocalDateTime reviewRequestedAt;

	@Column(nullable = false)
	private LocalDateTime dueAt;

	private Integer storyPoint;

	@Column(nullable = false)
	private int currentReviewRound = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT_ISSUE_ID")
	private Issue parentIssue;

	// TODO: Use Set collection
	@OneToMany(mappedBy = "parentIssue")
	private List<Issue> childIssues = new ArrayList<>();

	// TODO: Use Set collection
	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueRelation> outgoingRelations = new ArrayList<>();

	// TODO: Use Set collection
	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueRelation> incomingRelations = new ArrayList<>();

	// TODO: Use Set collection
	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueReviewer> reviewers = new ArrayList<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueAssignee> assignees = new HashSet<>();

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ISSUE_ID")
	private Set<IssueWatcher> watchers = new HashSet<>();

	// TODO: Use Set collection
	@OneToMany(mappedBy = "issue")
	private List<SprintIssue> sprintIssues = new ArrayList<>();

	protected Issue(
		Workspace workspace,
		IssueType type,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Integer storyPoint
	) {
		this.issueKey = workspace.getIssueKey();
		workspace.increaseNextIssueNumber();

		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		workspace.getIssues().add(this);

		this.type = type;
		this.title = title;
		this.content = content;
		this.summary = summary;
		this.status = TODO;
		this.priority = priority != null ? priority : IssuePriority.MEDIUM;
		this.dueAt = dueAt;
		this.storyPoint = storyPoint;
	}

	public boolean hasParent() {
		return parentIssue != null;
	}

	public void updateStoryPoint(Integer storyPoint) {
		this.storyPoint = storyPoint;
	}

	public IssueWatcher addWatcher(WorkspaceMember workspaceMember) {
		IssueWatcher watcher = new IssueWatcher(workspaceMember);
		watchers.add(watcher);
		return watcher;
	}

	public void removeWatcher(WorkspaceMember workspaceMember) {
		watchers.removeIf(watcher -> watcher.getWatcher().equals(workspaceMember));
	}

	/**
	 * Todo
	 *  - 기존 N+1 문제 발생을 IssueXxx 엔티티에 id를 직접 컬럼으로 저장해서, lazy loading 관련 문제 회피
	 *  - 사용 시점에 Join Fetch 쿼리 사용하는 방식으로 해결할 수도 있음
	 */
	public Set<Long> getSubscriberMemberIds() {
		Set<Long> memberIds = new HashSet<>();

		// add memberId of author
		if (this.getCreatedBy() != null) {
			memberIds.add(this.getCreatedBy());
		}

		// add Assignee memberIds
		assignees.stream()
			.map(IssueAssignee::getAssigneeMemberId)  // 엔티티 로드 없이 ID 접근
			.forEach(memberIds::add);

		// add Reviewer memberIds
		reviewers.stream()
			.map(IssueReviewer::getReviewerMemberId)  // 엔티티 로드 없이 ID 접근
			.forEach(memberIds::add);

		// add Watcher memberIds
		watchers.stream()
			.map(IssueWatcher::getWatcherMemberId)  // 엔티티 로드 없이 ID 접근
			.forEach(memberIds::add);

		return memberIds;
	}

	public Set<Long> getReviewerMemberIds() {
		Set<Long> reviewerIds = new HashSet<>();

		reviewers.stream()
			.map(IssueReviewer::getReviewerMemberId)
			.forEach(reviewerIds::add);

		return reviewerIds;
	}

	public void requestReview() {
		validateReviewersExist();

		if (isNotFirstReviewRound()) {
			validateCanStartNewReviewRound();
		}
		this.currentReviewRound++;
		this.updateStatus(IN_REVIEW);
	}

	public IssueReviewer addReviewer(WorkspaceMember workspaceMember) {
		validateReviewerLimit();
		validateIsReviewer(workspaceMember);

		IssueReviewer reviewer = new IssueReviewer(workspaceMember, this);
		reviewers.add(reviewer);

		return reviewer;
	}

	public void removeReviewer(WorkspaceMember workspaceMember) {
		IssueReviewer issueReviewer = findIssueReviewer(workspaceMember);
		validateHasReviewForCurrentRound(issueReviewer);

		reviewers.remove(issueReviewer);
	}

	public void validateCanSubmitReview() {
		boolean isStatusNotInReview = status != IN_REVIEW;

		if (isStatusNotInReview) {
			throw new InvalidOperationException(
				String.format("Issue status must be IN_REVIEW to create a review. Current status: %s",
					status));
		}
	}

	private IssueReviewer findIssueReviewer(WorkspaceMember workspaceMember) {
		return reviewers.stream()
			.filter(r -> r.getReviewer().getId().equals(workspaceMember.getId()))
			.findFirst()
			.orElseThrow(() -> new ForbiddenOperationException(
				String.format("Not a reviewer assigned to this issue. workspaceMemberId: %d, displayName: %s",
					workspaceMember.getId(), workspaceMember.getDisplayName()))
			);
	}

	private void validateReviewersExist() {
		if (reviewers.isEmpty()) {
			throw new InvalidOperationException("Cannot request review if there are no assigned reviewers.");
		}
	}

	private void validateHasReviewForCurrentRound(IssueReviewer issueReviewer) {
		if (issueReviewer.hasReviewForRound(currentReviewRound)) {
			throw new InvalidOperationException(
				String.format(
					"Cannot remove reviewer that already has a review for the current round. Current round: %d",
					currentReviewRound));
		}
	}

	private void validateCanStartNewReviewRound() {
		// 현재 상태 검증
		boolean isStatusNotInReview = status != IN_REVIEW;

		if (isStatusNotInReview) {
			throw new InvalidOperationException(
				String.format(
					"Issue status must be IN_REVIEW to start a new review round. Current issue status: %s",
					status));
		}

		// check if all reviewers submitted a reivew for the current round
		boolean hasIncompleteReviews = reviewers.stream()
			.noneMatch(reviewer -> reviewer.hasReviewForRound(currentReviewRound));

		if (hasIncompleteReviews) {
			throw new InvalidOperationException(
				String.format("Reviewers that have not completed their review for this round exist. Current round: %d",
					currentReviewRound));
		}
	}

	private void validateReviewerLimit() {
		if (reviewers.size() >= MAX_REVIEWERS) {
			throw new InvalidOperationException(
				String.format("The max number of reviewers for a single issue is %d.", MAX_REVIEWERS));
		}
	}

	private void validateIsReviewer(WorkspaceMember workspaceMember) {
		boolean isAlreadyReviewer = reviewers.stream()
			.anyMatch(r -> r.getReviewer().getId().equals(workspaceMember.getId()));

		if (isAlreadyReviewer) {
			throw new InvalidOperationException(
				String.format("Workspace member is already a reviewer. workspaceMemberId: %d",
					workspaceMember.getId()));
		}
	}

	public void validateIssueTypeMatch(IssueType type) {
		boolean typeNotMatch = this.type != type;

		if (typeNotMatch) {
			throw new InvalidOperationException(
				String.format("Issue type does not match the needed type. Issue type: %s, Required type: %s",
					this.type, type));
		}
	}

	public IssueAssignee addAssignee(WorkspaceMember workspaceMember) {
		validateAssigneeLimit();
		validateBelongsToWorkspace(workspaceMember);

		IssueAssignee assignee = new IssueAssignee(this, workspaceMember);

		assignees.add(assignee);
		return assignee;
	}

	public void removeAssignee(WorkspaceMember assignee) {
		IssueAssignee issueAssignee = findIssueAssignee(assignee);
		assignees.remove(issueAssignee);
	}

	public void validateIsAssignee(Long memberId) {
		boolean isNotAssignee = !isAssignee(memberId);

		if (isNotAssignee) {
			throw new ForbiddenOperationException(
				String.format("Must be an assignee of this issue. workspace code: %s, issue key: %s, member id: %d",
					workspaceCode, issueKey, memberId));
		}
	}

	public void validateIsAssigneeOrAuthor(Long memberId) {
		if (isAssignee(memberId)) {
			return;
		}
		if (isAuthor(memberId)) {
			return;
		}
		throw new ForbiddenOperationException(
			String.format("Must be the author or an assignee of this issue. issueKey: %s", issueKey)
		);
	}

	private IssueAssignee findIssueAssignee(WorkspaceMember assignee) {
		return assignees.stream()
			.filter(ia -> ia.getAssignee().getId().equals(assignee.getId()))
			.findFirst()
			.orElseThrow(() -> new InvalidOperationException(
				String.format("Is not a assignee assigned to this issue. workspaceMemberId: %d, displayName: %s",
					assignee.getId(), assignee.getDisplayName()))
			);
	}

	private boolean isAssignee(Long memberId) {
		return assignees.stream()
			.anyMatch(issueAssignee -> Objects.equals(issueAssignee.getAssigneeMemberId(), memberId));
	}

	private boolean isAuthor(Long memberId) {
		return Objects.equals(getCreatedBy(), memberId);
	}

	private void validateAssigneeLimit() {
		if (assignees.size() >= MAX_ASSIGNEES) {
			throw new InvalidOperationException(
				String.format("The maximum number of assignees for a single issue is %d", MAX_ASSIGNEES));
		}
	}

	private void validateBelongsToWorkspace(WorkspaceMember workspaceMember) {
		boolean hasDifferentWorkspaceCode = !workspaceMember.getWorkspaceCode().equals(workspaceCode);

		if (hasDifferentWorkspaceCode) {
			throw new InvalidOperationException(String.format(
				"Assignee must belong to this workspace. expected: %s , actual: %s",
				workspaceMember.getWorkspaceCode(), workspaceCode));
		}
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateContent(String content) {
		this.content = content;
	}

	public void updateSummary(String summary) {
		this.summary = summary;
	}

	public void updateDueAt(LocalDateTime dueAt) {
		this.dueAt = dueAt;
	}

	public void updateStatus(IssueStatus newStatus) {
		validateStatusTransition(newStatus);
		this.status = newStatus;

		updateTimestamps(newStatus);
	}

	public void updatePriority(IssuePriority priority) {
		this.priority = priority;
	}

	public void updateParentIssue(Issue parentIssue) {
		validateParentIssue(parentIssue);
		removeParentRelationship();

		this.parentIssue = parentIssue;
		parentIssue.getChildIssues().add(this);
	}

	public void removeParentRelationship() {
		if (parentIssue != null) {
			parentIssue.getChildIssues().remove(this);
			parentIssue = null;
		}
	}

	public void validateCanRemoveParent() {
	}

	public boolean isNotFirstReviewRound() {
		return currentReviewRound != 0;
	}

	private void updateTimestamps(IssueStatus newStatus) {
		if (newStatus == IN_PROGRESS && startedAt == null) {
			startedAt = LocalDateTime.now();
			return;
		}
		if (newStatus == IN_REVIEW) {
			reviewRequestedAt = LocalDateTime.now();
			return;
		}
		if (newStatus == DONE) {
			resolvedAt = LocalDateTime.now();
		}
	}

	protected void validateStatusTransition(IssueStatus newStatus) {
		validateBasicTransition(newStatus);

		// validate special status transitions
		if (newStatus == DONE) {
			validateTransitionToDone();
		}
	}

	private void validateBasicTransition(IssueStatus newStatus) {
		Set<IssueStatus> allowedStatuses = getAllowedNextStatuses();

		boolean statusNotAllowed = !allowedStatuses.contains(newStatus);

		if (statusNotAllowed) {
			throw new InvalidOperationException(
				String.format("Cannot change status from %s to %s.", status, newStatus)
			);
		}
	}

	// TODO: probably needs logic improvement for changing the status to DONE
	private void validateTransitionToDone() {
		if (hasAnyChangesRequested()) {
			throw new InvalidOperationException("All reviews for current round must be approved or be a comment.");
		}

		validateBlockingIssuesAreDone();
	}

	private boolean hasAnyChangesRequested() {
		return reviewers.stream()
			.anyMatch(
				reviewer -> reviewer.getCurrentReviewStatus(currentReviewRound) == ReviewStatus.CHANGES_REQUESTED);
	}

	private void validateBlockingIssuesAreDone() {
		List<Issue> blockingIssues = incomingRelations.stream()
			.filter(relation -> relation.getRelationType() == IssueRelationType.BLOCKED_BY)
			.map(IssueRelation::getSourceIssue)
			.filter(issue -> issue.getStatus() != DONE)
			.toList();

		if (!blockingIssues.isEmpty()) {
			String blockingIssueKeys = blockingIssues.stream()
				.map(Issue::getIssueKey)
				.collect(Collectors.joining(", "));

			throw new InvalidOperationException(
				String.format("Cannot complete this issue. Blocking issues must be completed first: %s",
					blockingIssueKeys));
		}
	}

	private Set<IssueStatus> getAllowedNextStatuses() {
		return switch (status) {
			case TODO -> Set.of(IN_PROGRESS, CLOSED, DELETED);
			case IN_PROGRESS -> Set.of(IN_REVIEW, DONE, CLOSED, DELETED);
			case IN_REVIEW -> Set.of(DONE);
			case DONE, CLOSED, DELETED -> Set.of();
		};
	}

	protected abstract void validateParentIssue(Issue parentIssue);

}
