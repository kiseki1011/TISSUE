package com.tissue.api.issue.domain.types;

import java.time.LocalDateTime;
import java.util.Objects;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
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
		LocalDateTime dueAt,
		Integer storyPoint,
		String businessGoal
	) {
		super(workspace, IssueType.EPIC, title, content, summary, priority, dueAt, storyPoint);

		this.businessGoal = businessGoal;
	}

	public void updateStoryPoint() {
		super.updateStoryPoint(calculateTotalStoryPoints());
	}

	/**
	 * Todo
	 *  - 현재 Epic의 자식 이슈들을 읽어오는 과정에 다음의 문제들이 발생할 수 있음
	 *  - 문제1: N+1 문제
	 *  - 문제2: 자식 이슈가 많은 경우 메모리 문제
	 *  - 해결 방법 -> Join Fetch, 집계용 쿼리 사용
	 *  - 그런데 일반적으로 권장되는 Epic의 자식 수는 10개 이하의 유저 스토리(user story)로 분해되는 것
	 *  - 자식 수가 몇 백개가 넘어가는 상황은 거의 없을 것으로 예상하기 때문에, Join Fetch로 리팩토링 후에
	 *  필요하면 집계용 쿼리를 사용하는 방식으로 변경하면 될 것 같음
	 */
	public Integer calculateTotalStoryPoints() {
		return getChildIssues().stream()
			.filter(issue -> issue.getStatus() != IssueStatus.CLOSED)
			.map(Issue::getStoryPoint)
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
