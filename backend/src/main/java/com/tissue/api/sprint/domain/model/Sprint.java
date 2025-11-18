package com.tissue.api.sprint.domain.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;
import com.tissue.api.sprint.exception.InvalidSprintDateException;
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
	private Set<SprintIssue> sprintIssues = new HashSet<>();

	@Builder
	public Sprint(
		String title,
		String goal,
		Instant plannedStartDate,
		Instant plannedEndDate,
		Workspace workspace
	) {
		ensureStartDateBeforeEndDate(plannedStartDate, plannedEndDate);

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

	public void updateDates(Instant startDate, Instant endDate) {
		ensureStartDateBeforeEndDate(startDate, endDate);
		this.plannedStartDate = startDate;
		this.plannedEndDate = endDate;
	}

	private void ensureStartDateBeforeEndDate(Instant startDate, Instant endDate) {
		if (endDate.isBefore(startDate)) {
			throw new InvalidSprintDateException(startDate, endDate, key);
		}
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

	private void validateStatusTransition(SprintStatus newStatus) {
		if (this.status == newStatus) {
			return;
		}

		switch (this.status) {
			case PLANNING -> {
				boolean newStatusIsNotActive = newStatus != SprintStatus.ACTIVE;
				boolean newStatusIsNotCancelled = newStatus != SprintStatus.CANCELLED;
				if (newStatusIsNotActive && newStatusIsNotCancelled) {
					// TODO: InvalidSprintStatusTransitionException
					throw new RuntimeException("Sprint in PLANNING status can only be changed to ACTIVE or CANCELLED.");
				}

				boolean newStatusIsActive = newStatus == SprintStatus.ACTIVE;
				if (newStatusIsActive) {
					boolean hasActiveSprintInWorkspace = workspace.hasActiveSprint();
					if (hasActiveSprintInWorkspace) {
						// TODO: ActiveSprintAlreadyExistsException, 더 괜찮은 이름이 있을까?
						throw new RuntimeException(
							"Cannot start sprint. A sprint is already active in this workspace.");
					}
				}
			}
			case ACTIVE -> {
				boolean newStatusIsNotCompleted = newStatus != SprintStatus.COMPLETED;
				boolean newStatusIsNotCancelled = newStatus != SprintStatus.CANCELLED;
				if (newStatusIsNotCompleted && newStatusIsNotCancelled) {
					// TODO: InvalidSprintStatusTransitionException
					throw new RuntimeException(
						"Sprint in ACTIVE status can only be changed to COMPLETED or CANCELLED.");
				}
			}
			case COMPLETED, CANCELLED ->
				// TODO: InvalidSprintStatusTransitionException
				throw new RuntimeException("Cannot change status of COMPLETED or CANCELLED sprint.");
		}
	}

	public void addIssue(Issue issue) {
		ensureCanAddIssue(issue);
		SprintIssue sprintIssue = new SprintIssue(this, issue);
		this.sprintIssues.add(sprintIssue);
	}

	public void removeIssue(Issue issue) {
		ensureCanRemoveIssue(issue);
		this.sprintIssues.removeIf(si -> si.getIssue().equals(issue));
	}

	private void ensureCanAddIssue(Issue issue) {
		boolean notRequiredStatus = status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE;
		if (notRequiredStatus) {
			// TODO: SprintNotModifiableException
			throw new RuntimeException("Can only add issues to PLANNING or ACTIVE sprint.");
		}

		// TODO: ensureFromSameWorkspace
		boolean notEqualWorkspaceCode = !issue.getWorkspaceKey().equals(workspace.getKey());
		if (notEqualWorkspaceCode) {
			// TODO: CrossWorkspaceIssueNotAllowedException
			throw new RuntimeException("Cannot add issue from different workspace to sprint.");
		}
	}

	private void ensureCanRemoveIssue(Issue issue) {
		boolean notRequiredStatus = status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE;
		// TODO: SprintNotModifiableException
		if (notRequiredStatus) {
			throw new RuntimeException("Can only remove issues from PLANNING or ACTIVE sprint.");
		}
	}
}
