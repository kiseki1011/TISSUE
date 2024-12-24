package com.tissue.api.issue.domain.types;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.Difficulty;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueStatus;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.issue.exception.CannotPauseCriticalBugException;
import com.tissue.api.issue.exception.ParentMustBeEpicException;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@DiscriminatorValue("BUG")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Bug extends Issue {

	/**
	 * Todo
	 *  - 버그 발생 환경에 대한 태그 Enviroment를 만들기
	 *    - ex. chrome-browser, python3.11, java8-jetbrains...
	 */

	private static final int CRITICAL_BUG_LEVEL = BugSeverity.CRITICAL.getLevel();

	@Lob
	@Column(nullable = false)
	private String reproducingSteps;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private BugSeverity severity;

	@ElementCollection
	@CollectionTable(
		name = "bug_affected_versions",
		joinColumns = @JoinColumn(name = "bug_id")
	)
	private Set<String> affectedVersions = new HashSet<>();

	private Difficulty difficulty;

	@Builder
	public Bug(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDate dueDate,
		Issue parentIssue,
		String reproducingSteps,
		BugSeverity severity,
		Set<String> affectedVersions,
		Difficulty difficulty
	) {
		super(workspace, IssueType.BUG, title, content, summary, priority, dueDate);
		this.reproducingSteps = reproducingSteps;
		this.severity = severity != null ? severity : BugSeverity.MINOR;

		updatePriorityByBugSeverity();

		if (affectedVersions != null) {
			this.affectedVersions.addAll(affectedVersions);
		}

		this.difficulty = difficulty;

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

	@Override
	protected void validateStatusTransition(IssueStatus newStatus) {
		super.validateStatusTransition(newStatus);
		if (needsImmediateAttention() && newStatus == IssueStatus.PAUSED) {
			throw new CannotPauseCriticalBugException();
		}
	}

	public boolean needsImmediateAttention() {
		return severity.getLevel() >= CRITICAL_BUG_LEVEL;
	}

	public void updatePriorityByBugSeverity() {
		if (severity.isMoreSevereThan(BugSeverity.CRITICAL)) {
			this.updatePriority(IssuePriority.EMERGENCY);
			return;
		}
		if (severity.isMoreSevereThan(BugSeverity.MAJOR)) {
			this.updatePriority(IssuePriority.HIGHEST);
		}
	}
}
