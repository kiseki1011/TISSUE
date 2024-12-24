package com.tissue.api.issue.domain.types;

import java.time.LocalDate;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.exception.SameHierarchyParentException;
import com.tissue.api.issue.exception.SubTaskNoParentException;
import com.tissue.api.issue.exception.SubTaskWrongParentTypeException;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("SUB_TASK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubTask extends Issue {

	private Difficulty difficulty;

	@Builder
	public SubTask(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate,
		Issue parentIssue,
		Difficulty difficulty
	) {
		super(workspace, IssueType.SUB_TASK, title, content, summary, priority, dueDate);
		this.difficulty = difficulty;

		if (parentIssue != null) {
			validateParentIssue(parentIssue);
			setParentIssue(parentIssue);
		}
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		// SubTask는 반드시 부모 이슈가 있어야 함, 그리고 반드시 Task/Story/Bug의 자식 이슈
		if (parentIssue == null) {
			throw new SubTaskNoParentException();
		}
		if ((parentIssue instanceof Epic)) {
			throw new SubTaskWrongParentTypeException();
		}
		if (parentIssue instanceof SubTask) {
			throw new SameHierarchyParentException();
		}
	}
}
