package com.tissue.api.issue.domain.types;

import java.time.LocalDate;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.workspace.domain.Workspace;

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
		super(workspace, IssueType.TASK, title, content, summary, priority, dueDate);
		this.difficulty = difficulty;

		if (parentIssue != null) {
			updateParentIssue(parentIssue);
		}
	}

	public void updateDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new InvalidOperationException("TASK type issues can only have an EPIC as their parent issue.");
		}
	}

}
