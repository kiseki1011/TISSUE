package com.uranus.taskmanager.api.issue.domain.types;

import java.time.LocalDate;

import com.uranus.taskmanager.api.issue.domain.Issue;
import com.uranus.taskmanager.api.issue.domain.enums.Difficulty;
import com.uranus.taskmanager.api.issue.domain.enums.IssuePriority;
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

	/**
	 * Todo
	 *  - parentIssue를 위해서 Epic epic 필드를 추가하는 것이 좋을까?
	 *    - 생성자의 parentIssue 대신 this.epic을 넣으면 됨
	 *  - 마찬가지로 childIssue를 위해서 List<SubTask> subTasks 필드를 추가할까?
	 *  -> 근데 생각해보니깐 다 별로인 듯... -> 그냥 조인해서 가져오지 뭐
	 */

	@Column(nullable = false)
	private String userValue;

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
		Issue parentIssue,
		String userValue,
		String acceptanceCriteria,
		Difficulty difficulty
	) {
		super(workspace, title, content, summary, priority, dueDate, parentIssue);
		this.userValue = userValue;
		this.acceptanceCriteria = acceptanceCriteria;
		this.difficulty = difficulty;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new ParentMustBeEpicException();
		}
	}
}
