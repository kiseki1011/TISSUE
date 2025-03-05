package com.tissue.api.issue.domain.types;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.BugSeverity;
import com.tissue.api.issue.domain.enums.IssuePriority;
import com.tissue.api.issue.domain.enums.IssueType;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.persistence.CollectionTable;
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

	private static final int CRITICAL_BUG_LEVEL = BugSeverity.CRITICAL.getLevel();

	@Lob
	private String reproducingSteps;

	@Enumerated(EnumType.STRING)
	private BugSeverity severity;

	@ElementCollection
	@CollectionTable(
		name = "bug_affected_versions",
		joinColumns = @JoinColumn(name = "bug_id")
	)
	private Set<String> affectedVersions = new HashSet<>();

	@Builder
	public Bug(
		Workspace workspace,
		String title,
		String content,
		String summary,
		IssuePriority priority,
		LocalDateTime dueAt,
		Integer storyPoint,
		Issue parentIssue,
		String reproducingSteps,
		BugSeverity severity,
		Set<String> affectedVersions
	) {
		super(workspace, IssueType.BUG, title, content, summary, priority, dueAt, storyPoint);

		this.reproducingSteps = reproducingSteps;
		this.severity = severity;

		updatePriorityByBugSeverity();

		if (affectedVersions != null) {
			this.affectedVersions.addAll(affectedVersions);
		}

		if (parentIssue != null) {
			updateParentIssue(parentIssue);
		}
	}

	public void updateReproducingSteps(String reproducingSteps) {
		this.reproducingSteps = reproducingSteps;
	}

	public void updateSeverity(BugSeverity severity) {
		this.severity = severity;
	}

	public void updateAffectedVersions(Set<String> affectedVersions) {
		this.affectedVersions = affectedVersions;
	}

	@Override
	protected void validateParentIssue(Issue parentIssue) {
		if (!(parentIssue instanceof Epic)) {
			throw new InvalidOperationException("BUG type issues can only have an EPIC as their parent issue.");
		}
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
