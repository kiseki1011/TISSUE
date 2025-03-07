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
@DiscriminatorValue("STORY")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Story extends Issue {

	private String userStory;
	private String acceptanceCriteria;

	@Builder
	public Story(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Integer storyPoint,
		Issue parentIssue,
		String userStory,
		String acceptanceCriteria
	) {
		super(workspace, IssueType.STORY, title, content, summary, priority, dueAt, storyPoint);

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

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new InvalidOperationException("STORY type issues can only have an EPIC as their parent issue.");
		}
	}
}
