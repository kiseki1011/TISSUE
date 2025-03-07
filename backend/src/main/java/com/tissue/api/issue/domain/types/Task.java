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
@DiscriminatorValue("TASK")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends Issue {

	@Builder
	public Task(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Integer storyPoint,
		Issue parentIssue
	) {
		super(workspace, IssueType.TASK, title, content, summary, priority, dueAt, storyPoint);

		if (parentIssue != null) {
			updateParentIssue(parentIssue);
		}
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new InvalidOperationException("TASK type issues can only have an EPIC as their parent issue.");
		}
	}

}
