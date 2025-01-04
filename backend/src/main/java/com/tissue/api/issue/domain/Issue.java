package com.tissue.api.issue.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.exception.UpdateIssueInReviewStatusException;
import com.tissue.api.issue.exception.UpdateStatusToInReviewException;
import com.tissue.api.review.domain.IssueReviewer;
import com.tissue.api.review.exception.NoReviewersAssignedException;
import com.tissue.api.review.exception.ReviewerAlreadyExistsException;
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

	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT_ISSUE_ID")
	private Issue parentIssue;

	@OneToMany(mappedBy = "parentIssue")
	private final List<Issue> childIssues = new ArrayList<>();

	// ---Review 도메인 관련 코드---
	@Column(nullable = false)
	private int currentReviewRound = 0;

	// @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
	// private final List<IssueReviewer> reviewers = new ArrayList<>();

	@OneToMany(cascade = CascadeType.ALL)
	@JoinColumn(name = "issue_id")
	private List<IssueReviewer> reviewers = new ArrayList<>();

	public void requestReview() {
		if (reviewers.isEmpty()) {
			throw new NoReviewersAssignedException();
		}

		this.currentReviewRound++;
		this.updateStatus(IssueStatus.IN_REVIEW);
	}

	public void addReviewer(WorkspaceMember reviewer) {
		boolean alreadyExists = reviewers.stream()
			.anyMatch(r -> r.getReviewer().equals(reviewer));

		if (alreadyExists) {
			throw new ReviewerAlreadyExistsException();
		}

		// reviewers.add(new IssueReviewer(this, reviewer));
		reviewers.add(new IssueReviewer(reviewer));
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

		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		workspace.getIssues().add(this);

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
		// validateStatusTransition(newStatus);
		this.status = newStatus;
		updateTimestamps(newStatus);
	}

	public void updatePriority(IssuePriority priority) {
		this.priority = priority;
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

	public void setParentIssue(Issue parentIssue) {
		validateParentIssue(parentIssue);
		removeParentRelationship();

		this.parentIssue = parentIssue;
		parentIssue.getChildIssues().add(this);
	}

	protected void validateStatusTransition(IssueStatus newStatus) {
		if (this.status == IssueStatus.IN_REVIEW) {
			throw new UpdateIssueInReviewStatusException();
		}
		if (newStatus == IssueStatus.IN_REVIEW) {
			throw new UpdateStatusToInReviewException();
		}
	}

	private void updateTimestamps(IssueStatus newStatus) {
		if (newStatus == IssueStatus.IN_PROGRESS && this.startedAt == null) {
			this.startedAt = LocalDateTime.now();
			return;
		}
		if (newStatus == IssueStatus.DONE) {
			this.finishedAt = LocalDateTime.now();
		}
	}

	protected abstract void validateParentIssue(Issue parentIssue);

}
