package com.tissue.api.issue.domain;

import static com.tissue.api.issue.domain.enums.IssueStatus.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tissue.api.assignee.domain.IssueAssignee;
import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.common.exception.type.ForbiddenOperationException;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueRelationType;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.sprint.domain.SprintIssue;
import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspacemember.domain.WorkspaceMember;

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

/**
 * Todo 3
 *  - 동시성 문제 해결을 위해서 이슈 생성에 spring-retry 적용
 *  - Workspace에서 issueKeyPrefix와 nextIssueNumber를 관리하기 때문에,
 *  Workspace에 Optimistic locking을 적용한다
 * Todo 4
 *  - 상태 업데이트는 도메인 이벤트(Domain Event) 발행으로 구현하는 것을 고려
 *  - 상태 변경과 관련된 부가 작업들(알림 발송, 감사 로그 기록 등)을 이벤트 핸들러에서 처리할 수 있어 확장성이 좋아짐
 * Todo 5
 *  - 상태 패턴(State, State Machine Pattern)의 사용 고려
 *  - 상태 변경 규칙을 한 곳에서 명확하게 관리할 수 있고, 새로운 상태나 규칙을 추가하기도 쉬워짐
 * Todo 6
 *  - 이슈 상태 변화에 대한 검증을 그냥 validator 클래스에서 정의해서 서비스에서 진행 고려
 */
@Entity
@Getter
@EqualsAndHashCode(of = {"issueKey", "workspaceCode"}, callSuper = false)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Issue extends WorkspaceContextBaseEntity {

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

	@OneToMany(mappedBy = "parentIssue")
	private List<Issue> childIssues = new ArrayList<>();

	@OneToMany(mappedBy = "sourceIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueRelation> outgoingRelations = new ArrayList<>();

	@OneToMany(mappedBy = "targetIssue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueRelation> incomingRelations = new ArrayList<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<IssueReviewer> reviewers = new ArrayList<>();

	@OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	private Set<IssueAssignee> assignees = new HashSet<>();

	/**
	 * Todo
	 *  - 단방향 관계를 위한 설정
	 *  - Assignee, Reviewer도 단방향 관계로 리팩토링하는게 좋을 듯
	 *    - 이유?: 굳이 탐색이 편하자고 양방향으로 만들 필요는 없을 듯
	 */
	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ISSUE_ID")
	private Set<IssueWatcher> watchers = new HashSet<>();

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
		validateBelongsToWorkspace(workspaceMember);

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
	public Set<Long> getSubscriberIds() {
		Set<Long> subscriberIds = new HashSet<>();

		// 작성자 ID 추가
		if (this.getCreatedByWorkspaceMember() != null) {
			subscriberIds.add(this.getCreatedByWorkspaceMember());
		}

		// Assignee IDs 추가
		assignees.stream()
			.map(IssueAssignee::getAssigneeId)  // 엔티티 로드 없이 ID 접근
			.forEach(subscriberIds::add);

		// Reviewer IDs 추가
		reviewers.stream()
			.map(IssueReviewer::getReviewerId)  // 엔티티 로드 없이 ID 접근
			.forEach(subscriberIds::add);

		// Watcher IDs 추가
		watchers.stream()
			.map(IssueWatcher::getWatcherId)  // 엔티티 로드 없이 ID 접근
			.forEach(subscriberIds::add);

		return subscriberIds;
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

	// TODO: 굳이 검사할 필요가 있을까? 그냥 MEMBER 이상이면 해제할 수 있도록 해도 괜찮지 않을까?
	public void validateCanRemoveReviewer(Long requesterWorkspaceMemberId, Long reviewerWorkspaceMemberId) {
		if (requesterWorkspaceMemberId.equals(reviewerWorkspaceMemberId)) {
			return;
		}

		boolean isNotAssignee = !isAssignee(requesterWorkspaceMemberId);

		if (isNotAssignee) {
			throw new ForbiddenOperationException(
				String.format("Must be the reviewer or be a assignee to remove the reviewer."
						+ " requesterWorkspaceMemberId: %d, reviewerWorkspaceMemberId: %d",
					requesterWorkspaceMemberId, reviewerWorkspaceMemberId));
		}
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
				String.format("Not a reviewer assigned to this issue. workspaceMemberId: %d, nickname: %s",
					workspaceMember.getId(), workspaceMember.getNickname()))
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
		boolean isStatusNotChangesRequested = status != CHANGES_REQUESTED;

		if (isStatusNotChangesRequested) {
			throw new InvalidOperationException(
				String.format(
					"Issue status must be CHANGES_REQUESTED to start a new review round. Current issue status: %s",
					status));
		}

		// 현재 라운드의 모든 리뷰어가 리뷰를 작성했는지 검증
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

	public void validateIsAssignee(Long workspaceMemberId) {
		boolean isNotAssignee = !isAssignee(workspaceMemberId);

		if (isNotAssignee) {
			throw new ForbiddenOperationException(
				String.format("Must be an assignee of this issue. issue key: %s, workspace member id: %d",
					issueKey, workspaceMemberId));
		}
	}

	public void validateIsAssigneeOrAuthor(Long workspaceMemberId) {
		if (isAssignee(workspaceMemberId)) {
			return;
		}
		if (isAuthor(workspaceMemberId)) {
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
				String.format("Is not a assignee assigned to this issue. workspaceMemberId: %d, nickname: %s",
					assignee.getId(), assignee.getNickname()))
			);
	}

	private boolean isAssignee(Long workspaceMemberId) {
		return assignees.stream()
			.anyMatch(issueAssignee -> issueAssignee.getAssignee().getId().equals(workspaceMemberId));
	}

	private boolean isAuthor(Long workspaceMemberId) {
		return this.getCreatedByWorkspaceMember().equals(workspaceMemberId);
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

	// TODO: 삭제 API를 상태 변경 API에서 분리하는 경우 사용
	// public void delete() {
	// 	validateStatusTransition(DELETED);
	// 	this.status = DELETED;
	// }

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

	/**
	 * Todo
	 *  - 설정 엔티티를 구현하면, forceReviewEnabled=true인 경우 다음 구현
	 *   - 리뷰가 강제되는 리뷰의 difficulty 수준을 설정 할 수 있도록 구현
	 *   - 리뷰를 하지 않아도 되는 priority 수준을 설정 할 수 있도록 구현
	 *   - 자식 이슈가 무조건 DONE이어야지 부모 이슈를 DONE으로 변경 가능하도록 만들기? -> 설정으로 제공할 수 있게 ㄱㄱ
	 *     - 구현한다면, "부모-자식"을 "BLOCKED_BY 자식-BLOCKS 부모" 관계가 자동으로 설정되도록 관계 설정 서비스도 같이 호출
	 */
	protected void validateStatusTransition(IssueStatus newStatus) {
		// 기본 상태 전이 검증
		validateBasicTransition(newStatus);

		// 특수한 상태 전이 검증
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

	// TODO: 이슈 상태를 DONE으로 변경하는 로직을 개선할 필요가 있음(리뷰 상태랑 관련된 로직도 개선 필요)
	private void validateTransitionToDone() {
		/*
		 * Todo: 워크스페이스 마다 가지는 설정을 관리하는 엔티티를 만들자.
		 *  - isForceReviewEnabled==true: DONE으로 변경하기 위해서는 리뷰어 등록, 모든 리뷰가 APPROVED이어야 함
		 */
		// if (!isForceReviewEnabled) {
		// 	return;
		// }
		if (hasAnyChangesRequested()) {
			throw new InvalidOperationException("All reviews for current round must not request change.");
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
			case IN_REVIEW -> Set.of(CHANGES_REQUESTED, DONE);
			case CHANGES_REQUESTED -> Set.of(IN_REVIEW);
			case DONE, CLOSED, DELETED -> Set.of();
		};
	}

	protected abstract void validateParentIssue(Issue parentIssue);

}
