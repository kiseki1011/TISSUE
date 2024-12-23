package com.tissue.api.issue.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.exception.UpdateIssueInReviewStatusException;
import com.tissue.api.issue.exception.UpdateStatusToInReviewException;
import com.tissue.api.workspace.domain.Workspace;

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

@Entity
@Getter
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Issue extends BaseEntity {

	@OneToMany(mappedBy = "parentIssue")
	private final List<Issue> childIssues = new ArrayList<>();
	/**
	 * Todo
	 *  - 이슈 마다 코드를 부여하자
	 *    - 순서대로 증가
	 *    - 워크스페이스 마다 고유
	 *    - 예시: EPIC-3, STORY-333, TASK-456, BUG-77, SUBTASK-1004
	 *    - 예시: ADMIN이상이 정하는 경우 -> TISSUE-1234
	 *  - dueDate가 null인 경우의 처리가 필요
	 * 	  - 예시: null이면 1주일 후의 날짜를 dueDate로 설정(생성자에서)
	 *  - parentIssue 추가하는 경우 해당 parentIssue에는 현재의 이슈가 childIssue로 추가
	 *    - 만약 Issue를 처음으로 생성하는 경우라면, 해당 Issue의 id는 어떻게 처리되더라?
	 *    - IDENTITY의 경우 DB에서 조회가 필요했던 것 같은데... 한번 찾아보자
	 *  - 리뷰어 신청을 해서 리뷰 상태 중 하나라도 PENDING이라면 이슈 status는 IN_REVIEW
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	// @Column(nullable = false, unique = true)
	// private String key;

	@Enumerated(EnumType.STRING)
	@Column(name = "type", insertable = false, updatable = false)
	private IssueType type;  // DB의 discriminator column과 매핑

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

	protected Issue(
		Workspace workspace,
		IssueType type,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate
	) {
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

	public void updateStatus(IssueStatus newStatus) {
		validateStatusTransition(newStatus);
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

	public void removeFromParent() {
		if (this.parentIssue != null) {
			this.parentIssue.getChildIssues().remove(this);
			this.parentIssue = null;
		}
	}

	protected void setParentIssue(Issue parentIssue) {
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
