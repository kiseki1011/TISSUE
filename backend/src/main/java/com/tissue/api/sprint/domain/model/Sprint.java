package com.tissue.api.sprint.domain.model;

import static com.tissue.api.common.util.DomainPreconditions.*;
import static com.tissue.api.sprint.domain.model.enums.SprintStatus.*;

import java.time.Instant;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.model.enums.SprintStatus;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// TODO: softDelete
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Sprint extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@Column(name = "project_key", nullable = false, updatable = false)
	private String projectKey;

	// TODO: size max = 50
	@Column(nullable = false)
	private String title;

	// TODO: size max = 255
	private String goal;

	private Instant startedAt;
	private Instant dueAt;
	private Instant completedAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SprintStatus status;

	public static Sprint create(
		@NonNull Project project,
		@NonNull String title,
		@Nullable String goal
	) {
		Sprint sprint = new Sprint();
		sprint.project = project;
		sprint.projectKey = project.getKey();
		sprint.title = title;
		sprint.goal = nullToEmpty(goal);
		sprint.status = PLANNING;

		return sprint;
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateGoal(@Nullable String goal) {
		this.goal = nullToEmpty(goal);
	}

	public void start(@NonNull Instant startedAt, @NonNull Instant dueAt) {
		if (this.status != PLANNING) {
			// TODO: InvalidSprintStatusException
			throw new IllegalStateException("Only PLANNING sprints can be started.");
		}

		ensureValidPeriod(startedAt, dueAt);

		this.status = ACTIVE;
		this.startedAt = startedAt;
		this.dueAt = dueAt;
	}

	public void complete() {
		if (this.status != ACTIVE) {
			// TODO: InvalidSprintStatusException
			throw new IllegalStateException("Only ACTIVE sprints can be completed.");
		}
		this.status = COMPLETED;
		this.completedAt = Instant.now();
	}

	private void ensureValidPeriod(Instant start, Instant end) {
		if (end.isBefore(start)) {
			// TODO: InvalidSprintDateException
			throw new IllegalStateException("Due date cannot be before start date.");
		}
	}
}
