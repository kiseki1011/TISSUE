package com.uranus.taskmanager.api.issue.domain.types;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
import com.uranus.taskmanager.api.issue.domain.enums.IssueType;
import com.uranus.taskmanager.api.issue.exception.ParentMustBeEpicException;
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
@DiscriminatorValue("STORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story extends Issue {

	@Column(name = "USER_STORY", nullable = false)
	private String userStory;

	private String acceptanceCriteria;

	private Difficulty difficulty;

	@Builder
	public Story(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate,
		Difficulty difficulty,
		Issue parentIssue,
		String userStory,
		String acceptanceCriteria
	) {
		super(workspace, IssueType.STORY, title, content, summary, priority, dueDate);
		this.difficulty = difficulty;
		this.userStory = userStory;
		this.acceptanceCriteria = acceptanceCriteria;

		if (parentIssue != null) {
			validateParentIssue(parentIssue);
			setParentIssue(parentIssue);
		}
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new ParentMustBeEpicException();
		}
	}
}
