package com.tissue.api.sprint.domain;

import java.time.LocalDateTime;
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
	private LocalDateTime plannedStartDate;

	@Column(nullable = false)
	private LocalDateTime plannedEndDate;

	private LocalDateTime startDate;
	private LocalDateTime endDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SprintStatus status;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "WORKSPACE_ID", nullable = false)
	private Workspace workspace;

	@Column(name = "WORKSPACE_CODE", nullable = false)
	private String workspaceCode;

	@Column(nullable = false, unique = true)
	private String sprintKey;

	@Column(nullable = false)
	private Integer number;

	@OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<SprintIssue> sprintIssues = new ArrayList<>();

	@Builder
	public Sprint(
		String title,
		String goal,
		LocalDateTime plannedStartDate,
		LocalDateTime plannedEndDate,
		Workspace workspace
	) {
		validateDates(plannedStartDate, plannedEndDate);

		this.number = workspace.getNextSprintNumber();
		this.sprintKey = String.format("SPRINT-%d", this.number);
		workspace.increaseNextSprintNumber();

		this.title = title;
		this.goal = goal;
		this.plannedStartDate = plannedStartDate;
		this.plannedEndDate = plannedEndDate;
		this.status = SprintStatus.PLANNING;
		this.workspace = workspace;
		this.workspaceCode = workspace.getCode();
	}

	public void updateTitle(String title) {
		this.title = title;
	}

	public void updateGoal(String goal) {
		this.goal = goal;
	}

	private void validateDates(LocalDateTime startDate, LocalDateTime endDate) {
		if (endDate.isBefore(startDate)) {
			throw new InvalidOperationException("Sprint end date cannot be before start date.");
		}
	}

	public void updateDates(LocalDateTime startDate, LocalDateTime endDate) {
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
			startDate = LocalDateTime.now();
			return;
		}
		if (newStatus == SprintStatus.COMPLETED) {
			endDate = LocalDateTime.now();
		}
	}

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
				if (newStatusIsActive && LocalDateTime.now().isAfter(plannedEndDate)) {
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

	private void validateCanAddIssue(Issue issue) {
		boolean notRequiredStatus = status != SprintStatus.PLANNING && status != SprintStatus.ACTIVE;
		if (notRequiredStatus) {
			throw new InvalidOperationException("Can only add issues to PLANNING or ACTIVE sprint.");
		}

		boolean notEqualWorkspaceCode = !issue.getWorkspaceCode().equals(this.workspaceCode);
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
