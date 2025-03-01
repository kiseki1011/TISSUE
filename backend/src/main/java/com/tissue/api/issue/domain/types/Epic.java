package com.tissue.api.issue.domain.types;

import java.time.LocalDate;

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
@DiscriminatorValue("EPIC")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Epic extends Issue {

	private String businessGoal;

	@Builder
	public Epic(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate,
		String businessGoal
	) {
		super(workspace, IssueType.EPIC, title, content, summary, priority, dueDate);
		this.businessGoal = businessGoal;
	}

	public void updateBusinessGoal(String businessGoal) {
		this.businessGoal = businessGoal;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		throw new InvalidOperationException("Epic type issues cannot have a parent issue.");
	}
}
