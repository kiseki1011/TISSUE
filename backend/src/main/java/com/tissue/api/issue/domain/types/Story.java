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
@DiscriminatorValue("STORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story extends Issue {

	private Difficulty difficulty;
	private String userStory;
	private String acceptanceCriteria;

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
			updateParentIssue(parentIssue);
		}
	}

	public void updateUserStory(String userStory) {
		this.userStory = userStory;
	}

	public void updateAcceptanceCriteria(String acceptanceCriteria) {
		this.acceptanceCriteria = acceptanceCriteria;
	}

	public void updateDifficulty(Difficulty difficulty) {
		this.difficulty = difficulty;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new InvalidOperationException("STORY type issues can only have an EPIC as their parent issue.");
		}
	}
}
