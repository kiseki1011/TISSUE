package com.uranus.taskmanager.api.issue.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseEntity;
import com.uranus.taskmanager.api.issue.exception.DirectUpdateToInReviewException;
import com.uranus.taskmanager.api.issue.exception.ParentIssueNotSameWorkspaceException;
import com.uranus.taskmanager.api.issue.exception.SubTaskParentIssueException;
import com.uranus.taskmanager.api.issue.exception.SubTaskWrongParentTypeException;
import com.uranus.taskmanager.api.issue.exception.UpdateIssueInReviewStatusException;
import com.uranus.taskmanager.api.issue.exception.WrongChildIssueTypeException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Issue extends BaseEntity {

	/**
	 * Todo
	 *  - 이슈 마다 코드를 부여하자
	 *  - 순서대로 증가
	 *  - 워크스페이스 마다 고유
	 *  - 예시: EPIC-3, STORY-333, TASK-456, BUG-77, SUBTASK-1004
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueType type; // Default: TASK

	@Column(nullable = false)
	private String title;

	@Lob
	@Column(nullable = false)
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueStatus status; // Default: TODO

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;  // Default: Medium

	// different from createdAt, must be updated implicitly
	private LocalDateTime startedAt; // updated when the first status change to IN_PROGRESS is made
	private LocalDateTime finishedAt; // updated when status change to DONE

	/**
	 * Todo
	 *  - dueDate가 null인 경우의 처리가 필요
	 *  - 예시: null이면 1주일 후의 날짜를 dueDate로 설정(생성자에서)
	 */
	private LocalDate dueDate;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT_ISSUE_ID")
	private Issue parentIssue;  // Epic의 하위 Story, Task의 하위 Sub-Task 등

	@OneToMany(mappedBy = "parentIssue")
	private List<Issue> childIssues = new ArrayList<>();

	@Builder
	public Issue(
		Workspace workspace,
		IssueType type,
		String title,
		String content,
		IssuePriority priority,
		LocalDateTime startedAt,
		LocalDateTime finishedAt,
		LocalDate dueDate,
		Issue parentIssue
	) {
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		workspace.getIssues().add(this);

		this.type = type != null ? type : IssueType.TASK;
		this.title = title;
		this.content = content;
		this.status = IssueStatus.TODO;
		this.priority = priority != null ? priority : IssuePriority.MEDIUM;

		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.dueDate = dueDate;

		/*
		 * Todo
		 *  - parentIssue 추가하는 경우 해당 parentIssue에는 현재의 이슈가 childIssue로 추가
		 *  - 만약 Issue를 처음으로 생성하는 경우라면, 해당 Issue의 id는 어떻게 처리되더라?
		 *  - IDENTITY의 경우 DB에서 조회가 필요했던 것 같은데... 한번 찾아보자
		 */
		if (parentIssue != null) {
			validateParentIssue(parentIssue);
			this.parentIssue = parentIssue;
			parentIssue.getChildIssues().add(this);
		}
	}

	public void updateStatus(IssueStatus newStatus) {
		// IN_REVIEW 상태인 이슈는 상태 변경 불가
		validateNotInReview();

		// IN_REVIEW로의 직접적인 상태 변경 불가
		validateNotUpdateToInReview(newStatus);

		// 상태별 특수 처리가 필요한 경우 해당 메서드 호출
		if (newStatus == IssueStatus.IN_PROGRESS) {
			updateStatusToInProgress();
			return;
		}

		if (newStatus == IssueStatus.DONE) {
			updateStatusToDone();
			return;
		}

		// 그 외의 경우 단순 상태 변경
		this.status = newStatus;
	}

	public void updateStatusToDone() {
		this.status = IssueStatus.DONE;
		this.finishedAt = LocalDateTime.now();
	}

	public void updateStatusToInProgress() {
		this.status = IssueStatus.IN_PROGRESS;
		if (this.startedAt == null) {
			this.startedAt = LocalDateTime.now();
		}
	}

	public void updateStatusToInReview() {
		this.status = IssueStatus.IN_REVIEW;
	}

	// Todo: 리뷰어 신청을 해서 리뷰 상태 중 하나라도 PENDING이라면 이슈 status는 IN_REVIEW

	public void addToWorkspace(Workspace workspace) {
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		workspace.getIssues().add(this);
	}

	public void removeFromParent() {
		if (this.parentIssue != null) {
			this.parentIssue.getChildIssues().remove(this);
			this.parentIssue = null;
		}
	}

	private void validateNotInReview() {
		if (this.status == IssueStatus.IN_REVIEW) {
			throw new UpdateIssueInReviewStatusException();
		}
	}

	private void validateNotUpdateToInReview(IssueStatus newStatus) {
		if (newStatus == IssueStatus.IN_REVIEW) {
			throw new DirectUpdateToInReviewException();
		}
	}

	private void validateParentIssue(Issue parentIssue) {
		// 동일한 워크스페이스에 속하는지 검증
		if (workspaceCodeIsDifferent(parentIssue)) {
			throw new ParentIssueNotSameWorkspaceException();
		}

		if (issueTypeIsSubTask(parentIssue)) {
			throw new SubTaskParentIssueException();
		}

		// 이슈 타입에 따른 부모-자식 관계 검증
		if (issueTypeIsSubTask(this) && (issueTypeIsEpic(parentIssue) || issueTypeIsSubTask(parentIssue))
		) {
			throw new SubTaskWrongParentTypeException();
		}

		if (issueTypeIsNotSubTask(this) && issueTypeIsNotEpic(parentIssue)) {
			throw new WrongChildIssueTypeException();
		}
	}

	private boolean issueTypeIsEpic(Issue issue) {
		return issue.type == IssueType.EPIC;
	}

	private boolean issueTypeIsNotEpic(Issue issue) {
		return issue.type != IssueType.EPIC;
	}

	private boolean issueTypeIsSubTask(Issue issue) {
		return issue.type == IssueType.SUB_TASK;
	}

	private boolean issueTypeIsNotSubTask(Issue issue) {
		return issue.type != IssueType.SUB_TASK;
	}

	private boolean workspaceCodeIsDifferent(Issue issue) {
		return !issue.getWorkspaceCode().equals(this.workspaceCode);
	}
}
