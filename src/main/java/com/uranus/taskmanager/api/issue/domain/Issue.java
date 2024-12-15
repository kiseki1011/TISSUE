package com.uranus.taskmanager.api.issue.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.uranus.taskmanager.api.common.entity.BaseEntity;
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
	private String description;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssueStatus status; // Default: TODO

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private IssuePriority priority;  // Default: Medium

	private LocalDateTime startedAt; // different from createdAt, must be updated implicitly
	private LocalDateTime finishedAt; // updated when status change to DONE

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
		String description,
		IssuePriority priority,
		LocalDateTime startedAt,
		LocalDateTime finishedAt,
		Issue parentIssue
	) {
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
		this.type = type != null ? type : IssueType.TASK;
		this.title = title;
		this.description = description;
		this.status = IssueStatus.TODO;
		this.priority = priority != null ? priority : IssuePriority.MEDIUM;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		if (parentIssue != null) {
			validateParentIssue(parentIssue);
			this.parentIssue = parentIssue;
			parentIssue.getChildIssues().add(this);
		}
	}

	private void validateParentIssue(Issue parentIssue) {
		// 동일한 워크스페이스에 속하는지 검증
		if (!parentIssue.getWorkspaceCode().equals(this.workspaceCode)) {
			// Todo: 커스텀 예외 만들기, ParentIssueNotSameWorkspaceException
			throw new IllegalArgumentException("Parent issue must belong to the same workspace");
		}

		// 이슈 타입에 따른 부모-자식 관계 검증
		if (this.type == IssueType.SUB_TASK
			&& (parentIssue.getType() == IssueType.EPIC || parentIssue.getType() == IssueType.SUB_TASK)
		) {
			// Todo: SubTaskWrongParentTypeException
			throw new IllegalArgumentException("Sub-tasks can only have Story, Task, or Bug as parent");
		}

		if (this.type != IssueType.SUB_TASK &&
			parentIssue.getType() != IssueType.EPIC) {
			// Todo: WrongChildIssueTypeException
			throw new IllegalArgumentException("Only Epic can have non-subtask children");
		}
	}
}
