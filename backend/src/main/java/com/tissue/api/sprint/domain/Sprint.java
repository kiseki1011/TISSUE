package com.tissue.api.sprint.domain;

import static com.tissue.api.sprint.domain.enums.SprintStatus.*;

import java.time.Instant;

import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.BaseEntity;
import com.tissue.api.project.domain.Project;
import com.tissue.api.sprint.domain.enums.SprintStatus;

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

	@Column(name = "workspace_key", nullable = false, updatable = false)
	private String workspaceKey;

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
		sprint.workspaceKey = project.getWorkspaceKey();
		sprint.title = title;
		sprint.goal = goal;
		sprint.status = PLANNING;

		return sprint;
	}

	public boolean isCompleted() {
		return status == COMPLETED;
	}

	public void updateTitle(@NonNull String title) {
		this.title = title;
	}

	public void updateGoal(@Nullable String goal) {
		this.goal = goal;
	}

	public void updateStartedAt(@NonNull Instant startedAt) {
		if (this.dueAt != null) {
			ensureValidPeriod(startedAt, this.dueAt);
		}
		this.startedAt = startedAt;
	}

	public void updateDueAt(@NonNull Instant dueAt) {
		if (this.startedAt != null) {
			ensureValidPeriod(this.startedAt, dueAt);
		}
		this.dueAt = dueAt;
	}

	// TODO: 호출 전에 Project에 활성(ACTIVE) Sprint가 없다는 것을 보장해야 함
	//  - 애플리케이션 계층에서 레포지토리 메서드를 통해 ACTIVE인 스프린트의 존재 여부 확인
	public void start(@NonNull Instant startedAt, @NonNull Instant dueAt) {
		if (this.status != PLANNING) {
			// TODO: InvalidSprintStatusException
			throw new IllegalStateException("Only PLANNING sprints can be started.");
		}

		ensureValidPeriod(startedAt, dueAt);

		this.startedAt = startedAt;
		this.dueAt = dueAt;

		this.status = ACTIVE;
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
