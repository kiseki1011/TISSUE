package com.tissue.api.sprint.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.workspace.domain.model.Workspace;

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
public class Sprint extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false)
	private String goal;

	@Column(nullable = false)
	private Instant plannedStartDate;

	@Column(nullable = false)
	private Instant plannedEndDate;

	private Instant startDate;
	private Instant endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SprintStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "sprint_key", nullable = false, unique = true)
	private String key;

	@OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SprintIssue> sprintIssues = new ArrayList<>();

	@Builder
	public Sprint(
		String title,
		String goal,
		Instant plannedStartDate,
		Instant plannedEndDate,
		Workspace workspace
	) {
		validateDates(plannedStartDate, plannedEndDate);

		this.key = workspace.generateSprintKey();
		this.title = title;
		this.goal = goal;
		this.plannedStartDate = plannedStartDate;
		this.plannedEndDate = plannedEndDate;
		this.status = SprintStatus.PLANNING;
		this.workspace = workspace;
	}

	public String getWorkspaceKey() {
		return workspace.getKey();
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateGoal(String goal) {
		this.goal = goal;
	}

	private void validateDates(Instant startDate, Instant endDate) {
		if (endDate.isBefore(startDate)) {
			throw new InvalidOperationException("Sprint end date cannot be before start date.");
		}
	}

	public void updateDates(Instant startDate, Instant endDate) {
		validateDates(startDate, endDate);
		this.plannedStartDate = startDate;
		this.plannedEndDate = endDate;
	}

	public void updateStatus(SprintStatus newStatus) {
		validateStatusTransition(newStatus);
		this.status = newStatus;

		updateTimestamps(newStatus);
	}

	private void updateTimestamps(SprintStatus newStatus) {
		if (newStatus == SprintStatus.ACTIVE) {
			startDate = Instant.now();
			return;
		}
		if (newStatus == SprintStatus.COMPLETED) {
			endDate = Instant.now();
		}
	}

	// TODO: Move to validator
	private void validateStatusTransition(SprintStatus newStatus) {
		if (this.status == newStatus) {
			throw new InvalidOperationException(
				String.format("Sprint is already in %s status.", newStatus));
		}

		switch (this.status) {
			case PLANNING -> {
				boolean newStatusIsNotActive = newStatus != SprintStatus.ACTIVE;
				boolean newStatusIsNotCancelled = newStatus != SprintStatus.CANCELLED;
				if (newStatusIsNotActive && newStatusIsNotCancelled) {
					throw new InvalidOperationException(
						"Sprint in PLANNING status can only be changed to ACTIVE or CANCELLED.");
				}

				boolean newStatusIsActive = newStatus == SprintStatus.ACTIVE;
				if (newStatusIsActive && Instant.now().isAfter(plannedEndDate)) {
					throw new InvalidOperationException("Cannot start sprint after planned end date.");
				}

				if (newStatusIsActive) {
					boolean hasActiveSprintInWorkspace = workspace.hasActiveSprint();
					if (hasActiveSprintInWorkspace) {
						throw new InvalidOperationException(
							"Cannot start sprint. A sprint is already active in this workspace.");
					}
				}
			}
			case ACTIVE -> {
				boolean newStatusIsNotCompleted = newStatus != SprintStatus.COMPLETED;
				boolean newStatusIsNotCancelled = newStatus != SprintStatus.CANCELLED;
				if (newStatusIsNotCompleted && newStatusIsNotCancelled) {
					throw new InvalidOperationException(
						"Sprint in ACTIVE status can only be changed to COMPLETED or CANCELLED.");
				}
			}
			case COMPLETED, CANCELLED ->
				throw new InvalidOperationException("Cannot change status of COMPLETED or CANCELLED sprint.");
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

	// TODO: Consider moving to validator
	private void validateCanAddIssue(Issue issue) {
		boolean notRequiredStatus = status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE;
		if (notRequiredStatus) {
			throw new InvalidOperationException("Can only add issues to PLANNING or ACTIVE sprint.");
		}

		boolean notEqualWorkspaceCode = !issue.getWorkspaceKey().equals(workspace.getKey());
		if (notEqualWorkspaceCode) {
			throw new InvalidOperationException("Cannot add issue from different workspace to sprint.");
		}

		boolean alreadyInSprint = sprintIssues.stream().anyMatch(si -> si.getIssue().equals(issue));
		if (alreadyInSprint) {
			throw new InvalidOperationException("Issue already exists in this sprint.");
		}
	}

	private void validateCanRemoveIssue(Issue issue) {
		boolean notRequiredStatus = status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE;
		if (notRequiredStatus) {
			throw new InvalidOperationException("Can only remove issues from PLANNING or ACTIVE sprint.");
		}

		boolean issueNotInSprint = sprintIssues.stream().noneMatch(si -> si.getIssue().equals(issue));
		if (issueNotInSprint) {
			throw new InvalidOperationException("Issue does not exist in this sprint.");
		}
	}
}
