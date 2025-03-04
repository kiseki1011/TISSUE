package com.tissue.api.issue.domain.types;

import java.time.LocalDateTime;
import java.util.Objects;

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

	// Todo: 에픽의 자식 이슈들의 스토리 포인트 합산을 스토리 포인트로 가지도록 구현 필요!
	private Integer storyPoint;

	@Builder
	public Epic(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Integer storyPoint,
		String businessGoal
	) {
		super(workspace, IssueType.EPIC, title, content, summary, priority, dueAt);

		this.storyPoint = storyPoint;
		this.businessGoal = businessGoal;
	}

	public void updateStoryPoint() {
		this.storyPoint = calculateTotalStoryPoints();
	}

	public Integer calculateTotalStoryPoints() {
		return getChildIssues().stream()
			.map(issue -> {
				if (issue instanceof Story) {
					return ((Story)issue).getStoryPoint();
				}
				if (issue instanceof Task) {
					return ((Task)issue).getStoryPoint();
				}
				if (issue instanceof Bug) {
					return ((Bug)issue).getStoryPoint();
				}
				return 0;
			})
			.filter(Objects::nonNull)
			.reduce(0, Integer::sum);
	}

	public void updateBusinessGoal(String businessGoal) {
		this.businessGoal = businessGoal;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		throw new InvalidOperationException("Epic type issues cannot have a parent issue.");
	}
}
