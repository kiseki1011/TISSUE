package com.tissue.api.issue.domain.types;

import java.time.LocalDateTime;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
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
@DiscriminatorValue("SUB_TASK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SubTask extends Issue {

	@Builder
	public SubTask(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Issue parentIssue
	) {
		super(workspace, IssueType.SUB_TASK, title, content, summary, priority, dueAt);

		if (parentIssue != null) {
			updateParentIssue(parentIssue);
		}
	}

	@Override
	public void validateCanRemoveParent() {
		throw new InvalidOperationException(
			String.format(
				"Cannot remove the parent of this issue: issueKey=%s, issueType=%s",
				getIssueKey(), getType()
			)
		);
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (parentIssue == null) {
			throw new InvalidOperationException("SUB_TASK type issues must have a parent issue.");
		}
		if ((parentIssue instanceof Epic)) {
			throw new InvalidOperationException(
				"SUB_TASK type issues can only have a STORY, TASK, or BUG type as the parent issue.");
		}
		if (parentIssue instanceof SubTask) {
			throw new InvalidOperationException("SUB_TASK type is not allowed as a parent issue.");
		}
	}
}
