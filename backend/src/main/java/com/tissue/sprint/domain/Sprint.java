package com.tissue.sprint.domain;

import static com.tissue.sprint.domain.SprintStatus.ACTIVE;
import static com.tissue.sprint.domain.SprintStatus.COMPLETED;
import static com.tissue.sprint.domain.SprintStatus.PLANNING;

import com.tissue.common.entity.BaseEntity;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.exception.InvalidSprintPeriodException;
import com.tissue.sprint.domain.exception.InvalidSprintStatusTransitionException;
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
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

// TODO: Soft-delete vs Hard-delete
@Entity
@Getter
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

    @Column(nullable = false, length = 100)
    private String title;

    @Nullable
    @Column(name = "goal")
    private String goal;

    @Nullable
    @Column(name = "started_at")
    private Instant startedAt;

    @Nullable
    @Column(name = "due_at")
    private Instant dueAt;

    @Nullable
    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "sprint_status", nullable = false)
    private SprintStatus status;

    @SuppressWarnings("NullAway.Init")
    protected Sprint() {}

    public static Sprint create(Project project, String title, @Nullable String goal) {
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

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updateGoal(@Nullable String goal) {
        this.goal = goal;
    }

    public void updateStartedAt(Instant startedAt) {
        if (this.dueAt != null) {
            ensureValidPeriod(startedAt, this.dueAt);
        }
        this.startedAt = startedAt;
    }

    public void updateDueAt(Instant dueAt) {
        if (this.startedAt != null) {
            ensureValidPeriod(this.startedAt, dueAt);
        }
        this.dueAt = dueAt;
    }

    public void start(Instant dueAt) {
        if (this.status != PLANNING) {
            throw new InvalidSprintStatusTransitionException(this.status, PLANNING, ACTIVE);
        }

        this.startedAt = Instant.now();
        ensureValidPeriod(this.startedAt, dueAt);
        this.dueAt = dueAt;
        this.status = ACTIVE;
    }

    public void complete() {
        if (this.status != ACTIVE) {
            throw new InvalidSprintStatusTransitionException(this.status, ACTIVE, COMPLETED);
        }
        this.status = COMPLETED;
        this.completedAt = Instant.now();
    }

    private void ensureValidPeriod(Instant start, Instant end) {
        if (end.isBefore(start)) {
            throw new InvalidSprintPeriodException(start, end);
        }
    }
}
