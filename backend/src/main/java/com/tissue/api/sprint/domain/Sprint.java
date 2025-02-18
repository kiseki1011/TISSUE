package com.tissue.api.sprint.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.sprint.domain.enums.SprintStatus;
import com.tissue.api.workspace.domain.Workspace;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sprint extends WorkspaceContextBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String goal;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SprintStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@Column(nullable = false, unique = true)
	private String sprintKey;  // "SPRINT-1", "SPRINT-2"

	@Column(nullable = false)
	private Integer number;

	@OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SprintIssue> sprintIssues = new ArrayList<>();

	@Builder
	public Sprint(
		String title,
		String goal,
		LocalDate startDate,
		LocalDate endDate,
		Workspace workspace
	) {
		validateDates(startDate, endDate);

		this.number = workspace.getNextSprintNumber();
		this.sprintKey = String.format("SPRINT-%d", this.number);
		workspace.increaseNextSprintNumber();

		this.title = title;
		this.goal = goal;
		this.startDate = startDate;
		this.endDate = endDate;
		this.status = SprintStatus.PLANNING;
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
	}

	private void validateDates(LocalDate startDate, LocalDate endDate) {
		if (endDate.isBefore(startDate)) {
			throw new InvalidOperationException("Sprint end date cannot be before start date.");
		}
	}

	// public void start() {
	// 	validateCanStart();
	// 	this.status = SprintStatus.ACTIVE;
	// }
	//
	// public void complete() {
	// 	validateCanComplete();
	// 	this.status = SprintStatus.COMPLETED;
	// }
	//
	// public void cancel() {
	// 	validateCanCancel();
	// 	this.status = SprintStatus.CANCELLED;
	// }

	public void updateStatus(SprintStatus newStatus) {
		validateStatusTransition(newStatus);
		validateNoOtherActiveSprintExists(newStatus);
		this.status = newStatus;
	}

	private void validateStatusTransition(SprintStatus newStatus) {
		if (this.status == newStatus) {
			throw new InvalidOperationException(
				String.format("Sprint is already in %s status", newStatus)
			);
		}

		switch (this.status) {
			case PLANNING -> {
				if (newStatus != SprintStatus.ACTIVE && newStatus != SprintStatus.CANCELLED) {
					throw new InvalidOperationException(
						"Sprint in PLANNING status can only be changed to ACTIVE or CANCELLED"
					);
				}
				if (newStatus == SprintStatus.ACTIVE && LocalDate.now().isAfter(endDate)) {
					throw new InvalidOperationException("Cannot start sprint after end date");
				}
			}
			case ACTIVE -> {
				if (newStatus != SprintStatus.COMPLETED && newStatus != SprintStatus.CANCELLED) {
					throw new InvalidOperationException(
						"Sprint in ACTIVE status can only be changed to COMPLETED or CANCELLED"
					);
				}
			}
			case COMPLETED, CANCELLED -> throw new InvalidOperationException(
				"Cannot change status of COMPLETED or CANCELLED sprint"
			);
		}
	}

	private void validateNoOtherActiveSprintExists(SprintStatus newStatus) {
		if (newStatus == SprintStatus.ACTIVE) {
			boolean hasActiveSprintInWorkspace = workspace.hasActiveSprintExcept(this);
			if (hasActiveSprintInWorkspace) {
				throw new InvalidOperationException(
					"Cannot start sprint. A sprint is already active in this workspace.");
			}
		}
	}

	public void addIssue(Issue issue) {
		validateCanAddIssue(issue);
		SprintIssue sprintIssue = new SprintIssue(this, issue);
		this.sprintIssues.add(sprintIssue);
	}

	public void removeIssue(Issue issue) {
		validateCanRemoveIssue(issue);
		this.sprintIssues.removeIf(si -> si.getIssue().equals(issue));
	}

	// private void validateCanStart() {
	// 	if (status != SprintStatus.PLANNING) {
	// 		throw new InvalidOperationException("Can only start sprints in PLANNING status");
	// 	}
	// 	if (LocalDate.now().isAfter(endDate)) {
	// 		throw new InvalidOperationException("Cannot start sprint after end date");
	// 	}
	// }
	//
	// private void validateCanComplete() {
	// 	if (status != SprintStatus.ACTIVE) {
	// 		throw new InvalidOperationException("Can only complete ACTIVE sprints");
	// 	}
	// }
	//
	// private void validateCanCancel() {
	// 	if (status == SprintStatus.COMPLETED || status == SprintStatus.CANCELLED) {
	// 		throw new InvalidOperationException("Cannot cancel COMPLETED or CANCELLED sprints");
	// 	}
	// }

	private void validateCanAddIssue(Issue issue) {
		if (status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE) {
			throw new InvalidOperationException(
				"Can only add issues to sprints in PLANNING or ACTIVE status"
			);
		}
		if (!issue.getWorkspaceCode().equals(this.workspaceCode)) {
			throw new InvalidOperationException(
				"Cannot add issue from different workspace to sprint"
			);
		}
		boolean alreadyInSprint = sprintIssues.stream()
			.anyMatch(si -> si.getIssue().equals(issue));
		if (alreadyInSprint) {
			throw new InvalidOperationException("Issue is already in this sprint");
		}
	}

	private void validateCanRemoveIssue(Issue issue) {
		if (status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE) {
			throw new InvalidOperationException(
				"Can only remove issues from sprints in PLANNING or ACTIVE status"
			);
		}
		boolean issueNotInSprint = sprintIssues.stream()
			.noneMatch(si -> si.getIssue().equals(issue));
		if (issueNotInSprint) {
			throw new InvalidOperationException("Issue is not in this sprint");
		}
	}
}
