package com.tissue.api.issue.domain;

import static com.tissue.api.issue.domain.enums.IssueStatus.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.tissue.api.assignee.domain.IssueAssignee;
import com.tissue.api.assignee.exception.AssigneeNotFoundException;
import com.tissue.api.assignee.exception.DuplicateAssigneeException;
import com.tissue.api.assignee.exception.InvalidAssigneeException;
import com.tissue.api.assignee.exception.MaxAssigneesExceededException;
import com.tissue.api.assignee.exception.UnauthorizedAssigneeModificationException;
import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.exception.InvalidStatusTransitionException;
import com.tissue.api.issue.exception.UnauthorizedIssueModifyException;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.domain.enums.ReviewStatus;
import com.tissue.api.review.exception.CannotRemoveReviewerException;
import com.tissue.api.review.exception.DuplicateReviewerException;
import com.tissue.api.review.exception.IncompleteReviewRoundException;
import com.tissue.api.review.exception.IssueStatusNotChangesRequestedException;
import com.tissue.api.review.exception.IssueStatusNotInReviewException;
import com.tissue.api.review.exception.MaxReviewersExceededException;
import com.tissue.api.review.exception.NoReviewersAddedException;
import com.tissue.api.review.exception.PendingReviewExistsException;
import com.tissue.api.review.exception.ReviewRequiredException;
import com.tissue.api.review.exception.ReviewerNotFoundException;
import com.tissue.api.review.exception.UnauthorizedReviewerModificationException;
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
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Todo 1
 *  - workspaceCode + issueKey 복합 인덱스를 위한 필드 고려?
 *  - 모든 이슈의 조회를 workspaceCode + issueKey를 사용 중
 * <br>
 * Todo 2
 *  - dueDate가 null인 경우의 처리가 필요
 * 	  - 예시: null이면 1주일 후의 날짜를 dueDate로 설정(생성자에서)
 *  - parentIssue 추가하는 경우 해당 parentIssue에는 현재의 이슈가 childIssue로 추가
 *    - 만약 Issue를 처음으로 생성하는 경우라면, 해당 Issue의 id는 어떻게 처리되더라?
 *    - IDENTITY의 경우 DB에서 조회가 필요했던 것 같은데... 한번 찾아보자
 *  - 리뷰어 신청을 해서 리뷰 상태 중 하나라도 PENDING이라면 이슈 status는 IN_REVIEW
 * <br>
 * Todo 3
 *  - 동시성 문제 해결을 위해서 이슈 생성에 spring-retry 적용
 *  - Workspace에서 issueKeyPrefix와 nextIssueNumber를 관리하기 때문에,
 *  Workspace에 Optimistic locking을 적용한다
 * <br>
 * Todo 4
 *  - 상태 업데이트는 도메인 이벤트(Domain Event) 발행으로 구현하는 것을 고려
 *  - 상태 변경과 관련된 부가 작업들(알림 발송, 감사 로그 기록 등)을 이벤트 핸들러에서 처리할 수 있어 확장성이 좋아짐
 * <br>
 * Todo 5
 *  - 상태 패턴(State, State Machine Pattern)의 사용 고려
 *  - 상태 변경 규칙을 한 곳에서 명확하게 관리할 수 있고, 새로운 상태나 규칙을 추가하기도 쉬워짐
 * <br>
 * Todo 6
 *  - 이슈 상태 변화에 대한 검증을 그냥 validator 클래스에서 정의해서 서비스에서 진행 고려
 * <br>
 * Todo 7
 *  - difficulty를 Issue로 이동
 */
@Entity
@Getter
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Issue extends WorkspaceContextBaseEntity {

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
	@Column
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueStatus status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;

	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;
	private LocalDateTime reviewRequestedAt;

	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT_ISSUE_ID")
	private Issue parentIssue;

	@OneToMany(mappedBy = "parentIssue")
	private final List<Issue> childIssues = new ArrayList<>();

	// ---Review 도메인 관련 코드---
	private static final int MAX_REVIEWERS = 10;

	@Column(nullable = false)
	private int currentReviewRound = 0;

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "ISSUE_ID")
	private List<IssueReviewer> reviewers = new ArrayList<>();

	public void requestReview() {
		validateReviewersExist();

		if (isNotFirstReviewRound()) {
			validateCanStartNewReviewRound();
		}
		this.currentReviewRound++;
		this.updateStatus(IssueStatus.IN_REVIEW);
	}

	public void addReviewer(WorkspaceMember reviewer) {
		validateReviewerLimit();
		validateNotAlreadyReviewer(reviewer);

		reviewers.add(new IssueReviewer(reviewer));
	}

	public void removeReviewer(WorkspaceMember reviewer) {
		// 해당 reviewer의 IssueReviewer를 찾아 제거
		IssueReviewer issueReviewer = reviewers.stream()
			.filter(r -> r.getReviewer().getId().equals(reviewer.getId()))
			.findFirst()
			.orElseThrow(ReviewerNotFoundException::new);

		// 리뷰어가 이미 리뷰를 작성했는지 검증
		validateHasReviewForCurrentRound(issueReviewer);

		reviewers.remove(issueReviewer);
	}

	public void validateCanRemoveReviewer(Long requesterWorkspaceMemberId, Long reviewerWorkspaceMemberId) {
		// 자기 자신을 제거하는 경우는 바로 통과
		if (requesterWorkspaceMemberId.equals(reviewerWorkspaceMemberId)) {
			return;
		}

		// 작업자인 경우도 통과
		boolean isAssignee = isAssignee(requesterWorkspaceMemberId);

		if (!isAssignee) {
			throw new UnauthorizedReviewerModificationException(
				"Only the reviewer themselves or issue assignees can remove reviewers.");
		}
	}

	private void validateHasReviewForCurrentRound(IssueReviewer issueReviewer) {
		if (issueReviewer.hasReviewForRound(this.currentReviewRound)) {
			throw new CannotRemoveReviewerException(
				"Cannot remove reviewer who already wrote a review for current round.");
		}
	}

	private void validateReviewersExist() {
		if (reviewers.isEmpty()) {
			throw new NoReviewersAddedException();
		}
	}

	private void validateCanStartNewReviewRound() {
		// 현재 상태 검증
		if (this.status != IssueStatus.CHANGES_REQUESTED) {
			// Todo: InvalidIssueStatusException로 변경(메세지로 세부 사항 전달)
			throw new IssueStatusNotChangesRequestedException(
				String.format(
					"The issue status must be CHANGES_REQUESTED to start a new review round. Current issue status: %s",
					this.status
				)
			);
		}

		// 현재 라운드의 모든 리뷰어가 리뷰를 작성했는지 검증
		boolean allReviewersSubmitted = reviewers.stream()
			.allMatch(reviewer -> reviewer.hasReviewForRound(this.currentReviewRound));

		if (!allReviewersSubmitted) {
			throw new IncompleteReviewRoundException(
				String.format(
					"There are reviewers that have not completed their review for this round. Current round: (%d)",
					this.currentReviewRound
				)
			);
		}
	}

	private void validateReviewerLimit() {
		if (reviewers.size() >= MAX_REVIEWERS) {
			throw new MaxReviewersExceededException(
				String.format(
					"The max number of reviewers for a single issue is %d.",
					MAX_REVIEWERS
				)
			);
		}
	}

	private void validateNotAlreadyReviewer(WorkspaceMember reviewer) {
		boolean isAlreadyReviewer = reviewers.stream()
			.anyMatch(r -> r.getReviewer().getId().equals(reviewer.getId()));

		if (isAlreadyReviewer) {
			throw new DuplicateReviewerException();
		}
	}

	public void validateReviewIsCreateable() {
		if (this.getStatus() != IssueStatus.IN_REVIEW) {
			throw new IssueStatusNotInReviewException(); // Todo: InvalidIssueStatusException로 변경(메세지로 세부 사항 전달)
		}
	}

	// -----------------------------

	// --assignee 도메인 관련 코드--

	private static final int MAX_ASSIGNEES = 10;

	@OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "ISSUE_ID")
	private final List<IssueAssignee> assignees = new ArrayList<>();

	public void addAssignee(WorkspaceMember assignee) {
		validateAssigneeLimit();
		validateAssigneeBelongsToWorkspace(assignee);
		validateNotAlreadyAssigned(assignee);

		assignees.add(new IssueAssignee(assignee));
	}

	public void removeAssignee(WorkspaceMember assignee) {
		IssueAssignee issueAssignee = assignees.stream()
			.filter(ia -> ia.getAssignee().getId().equals(assignee.getId()))
			.findFirst()
			.orElseThrow(() -> new AssigneeNotFoundException(
				String.format("Assignee '%s' is not assigned to this issue", assignee.getNickname())
			));

		assignees.remove(issueAssignee);
	}

	public void validateIsAssignee(Long workspaceMemberId) {
		boolean isAssignee = isAssignee(workspaceMemberId);

		if (!isAssignee) {
			throw new UnauthorizedAssigneeModificationException(
				"You must be an assignee of the Issue.");
		}
	}

	public void validateIsAssigneeOrAuthor(Long workspaceMemberId) {
		if (isAssignee(workspaceMemberId) || isAuthor(workspaceMemberId)) {
			return;
		}
		throw new UnauthorizedIssueModifyException("Must be the author or a assignee of this issue.");
	}

	private boolean isAssignee(Long workspaceMemberId) {
		return assignees.stream()
			.anyMatch(issueAssignee ->
				issueAssignee.getAssignee().getId().equals(workspaceMemberId));
	}

	private boolean isAuthor(Long workspaceMemberId) {
		return this.getCreatedByWorkspaceMember().equals(workspaceMemberId);
	}

	private void validateAssigneeLimit() {
		if (assignees.size() >= MAX_ASSIGNEES) {
			throw new MaxAssigneesExceededException(
				String.format("The maximum number of assignees for a single issue is %d", MAX_ASSIGNEES)
			);
		}
	}

	private void validateAssigneeBelongsToWorkspace(WorkspaceMember assignee) {
		if (!assignee.getWorkspaceCode().equals(this.workspaceCode)) {
			throw new InvalidAssigneeException("Assignee must belong to the same workspace");
		}
	}

	private void validateNotAlreadyAssigned(WorkspaceMember assignee) {
		boolean isAlreadyAssigned = isAssignee(assignee.getId());

		if (isAlreadyAssigned) {
			throw new DuplicateAssigneeException(
				String.format("Member '%s' is already assigned to this issue", assignee.getNickname())
			);
		}
	}

	// -----------------------------

	protected Issue(
		Workspace workspace,
		IssueType type,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate
	) {
		this.issueKey = workspace.getIssueKey();
		workspace.increaseNextIssueNumber();

		addToWorkspace(workspace);

		this.type = type;
		this.title = title;
		this.content = content;
		this.summary = summary;
		this.status = IssueStatus.TODO;
		this.priority = priority != null ? priority : IssuePriority.MEDIUM;
		this.dueDate = dueDate;
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

	public void updateDueDate(LocalDate dueDate) {
		this.dueDate = dueDate;
	}

	public void updateStatus(IssueStatus newStatus) {
		validateStatusTransition(newStatus);
		this.status = newStatus;
		updateTimestamps(newStatus);
	}

	public void updatePriority(IssuePriority priority) {
		this.priority = priority;
	}

	public void setParentIssue(Issue parentIssue) {
		validateParentIssue(parentIssue);
		removeParentRelationship();

		this.parentIssue = parentIssue;
		parentIssue.getChildIssues().add(this);
	}

	public void addToWorkspace(Workspace workspace) {
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		workspace.getIssues().add(this);
	}

	public void removeParentRelationship() {
		if (this.parentIssue != null) {
			this.parentIssue.getChildIssues().remove(this);
			this.parentIssue = null;
		}
	}

	public boolean canRemoveParentRelationship() {
		return true;
	}

	public boolean isNotFirstReviewRound() {
		return this.getCurrentReviewRound() != 0;
	}

	// Todo: 횡단 관심사(cross cutting concern)는 AOP로 구현하는걸 고려하자
	private void updateTimestamps(IssueStatus newStatus) {
		if (newStatus == IN_PROGRESS && this.startedAt == null) {
			this.startedAt = LocalDateTime.now();
			return;
		}
		if (newStatus == IssueStatus.IN_REVIEW) {
			this.reviewRequestedAt = LocalDateTime.now();
			return;
		}
		if (newStatus == IssueStatus.DONE) {
			this.finishedAt = LocalDateTime.now();
		}
	}

	/**
	 * Todo
	 *  - 설정 엔티티를 구현하면, forceReviewEnabled=true인 경우 다음 구현
	 *   - 리뷰가 강제되는 리뷰의 difficulty 수준을 설정 할 수 있도록 구현
	 *   - 리뷰를 하지 않아도 되는 priority 수준을 설정 할 수 있도록 구현
	 */
	protected void validateStatusTransition(IssueStatus newStatus) {
		// 기본 상태 전이 검증
		validateBasicTransition(newStatus);

		// 특수한 상태 전이 검증
		if (newStatus == IssueStatus.DONE) {
			validateTransitionToDone();
		}
	}

	private void validateTransitionToDone() {
		/*
		 * Todo: 워크스페이스 마다 가지는 설정을 관리하는 엔티티를 만들자.
		 *  - isForceReviewEnabled==true: DONE으로 변경하기 위해서는 리뷰어 등록, 모든 리뷰가 APPROVED이어야 함
		 */
		// if (!isForceReviewEnabled) {
		// 	return;
		// }

		if (reviewers.isEmpty()) {
			throw new ReviewRequiredException("Review is required to complete this issue.");
		}

		if (isAllReviewsNotApproved()) {
			throw new PendingReviewExistsException("All reviews must be approved.");
		}
	}

	private boolean isAllReviewsNotApproved() {
		return !reviewers.stream()
			.allMatch(reviewer ->
				reviewer.getCurrentReviewStatus(currentReviewRound) == ReviewStatus.APPROVED
			);
	}

	private void validateBasicTransition(IssueStatus newStatus) {
		Set<IssueStatus> allowedStatuses = getAllowedNextStatuses();

		if (!allowedStatuses.contains(newStatus)) {
			throw new InvalidStatusTransitionException(
				String.format("Cannot transition from %s to %s.", this.status, newStatus)
			);
		}
	}

	private Set<IssueStatus> getAllowedNextStatuses() {
		return switch (this.status) {
			case TODO -> Set.of(IN_PROGRESS, PAUSED, CLOSED);
			case IN_PROGRESS -> Set.of(IN_REVIEW, PAUSED, DONE, CLOSED);
			case IN_REVIEW -> Set.of(CHANGES_REQUESTED, DONE);
			case CHANGES_REQUESTED -> Set.of(IN_REVIEW);
			case DONE -> Set.of();
			case PAUSED -> Set.of(IN_PROGRESS, CLOSED);
			case CLOSED -> Set.of();
		};
	}

	protected abstract void validateParentIssue(Issue parentIssue);

}
