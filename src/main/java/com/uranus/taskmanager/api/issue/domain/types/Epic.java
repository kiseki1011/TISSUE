package com.uranus.taskmanager.api.issue.domain.types;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.exception.EpicCannotHaveParentException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("EPIC")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Epic extends Issue {

	@Column(nullable = false)
	private String businessGoal; // 에픽의 목표

	private LocalDate targetReleaseDate; // 릴리즈 예정일
	private LocalDate hardDeadLine; // 협상 불가능한 마감일

	@Builder
	public Epic(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate,
		String businessGoal,
		LocalDate targetReleaseDate,
		LocalDate hardDeadLine
	) {
		super(workspace, title, content, summary, priority, dueDate, null);
		this.businessGoal = businessGoal;
		this.targetReleaseDate = targetReleaseDate;
		this.hardDeadLine = hardDeadLine;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		throw new EpicCannotHaveParentException();
	}
}
