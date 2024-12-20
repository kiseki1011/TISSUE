package com.uranus.taskmanager.api.issue.domain.types;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.exception.ParentMustBeEpicException;
import com.uranus.taskmanager.api.workspace.domain.Workspace;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("TASK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends Issue {

	/**
	 * Todo
	 *  - TechStack이라는 엔티티를 만들어서, 기술 스택을 관리
	 */

	private Difficulty difficulty;

	@Builder
	public Task(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate,
		Issue parentIssue,
		Difficulty difficulty
	) {
		super(workspace, title, content, summary, priority, dueDate, parentIssue);
		this.difficulty = difficulty;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new ParentMustBeEpicException();
		}
	}

}
