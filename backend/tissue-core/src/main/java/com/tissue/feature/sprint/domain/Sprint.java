package com.tissue.feature.sprint.domain;

import static com.tissue.feature.sprint.domain.SprintStatus.ACTIVE;
import static com.tissue.feature.sprint.domain.SprintStatus.CANCELLED;
import static com.tissue.feature.sprint.domain.SprintStatus.COMPLETED;
import static com.tissue.feature.sprint.domain.SprintStatus.PLANNING;
import static com.tissue.feature.sprint.domain.exception.SprintErrorCode.INVALID_SPRINT_PERIOD;
import static com.tissue.feature.sprint.domain.exception.SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_sprint_number",
                    columnNames = {"project_id", "sprint_number"})
        })
@SQLRestriction("soft_deleted = false")
public class Sprint extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

    @Column(name = "sprint_number", nullable = false, updatable = false)
    private Long sprintNumber;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "goal")
    private String goal = "";

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
        sprint.ensureEditable();
        sprint.projectKey = project.getKey();
        sprint.sprintNumber = project.generateNextSprintNumber();
        sprint.title = title;
        sprint.goal = Objects.requireNonNullElse(goal, "");
        sprint.status = PLANNING;

        return sprint;
    }

    public String getSprintKey() {
        return "S-" + sprintNumber;
    }

    public boolean isCompleted() {
        return status == COMPLETED;
    }

    public boolean isCancelled() {
        return status == CANCELLED;
    }

    public void updateTitle(String title) {
        ensureEditable();
        this.title = title;
    }

    public void updateGoal(@Nullable String goal) {
        ensureEditable();
        this.goal = Objects.requireNonNullElse(goal, "");
    }

    public void updateStartedAt(Instant startedAt) {
        ensureEditable();
        if (this.dueAt != null) {
            ensureValidPeriod(startedAt, this.dueAt);
        }
        this.startedAt = startedAt;
    }

    public void updateDueAt(Instant dueAt) {
        ensureEditable();
        if (this.startedAt != null) {
            ensureValidPeriod(this.startedAt, dueAt);
        }
        this.dueAt = dueAt;
    }

    public void start(Instant dueAt) {
        ensureEditable();
        if (this.status != PLANNING) {
            throw new BadRequestException(INVALID_SPRINT_STATUS_TRANSITION);
        }

        this.startedAt = Instant.now();
        ensureValidPeriod(this.startedAt, dueAt);
        this.dueAt = dueAt;
        this.status = ACTIVE;
    }

    public void complete() {
        ensureEditable();
        if (this.status != ACTIVE) {
            throw new BadRequestException(INVALID_SPRINT_STATUS_TRANSITION);
        }
        this.status = COMPLETED;
        this.completedAt = Instant.now();
    }

    public void cancel() {
        ensureEditable();
        if (this.status == COMPLETED) {
            throw new BadRequestException(INVALID_SPRINT_STATUS_TRANSITION);
        }
        this.status = CANCELLED;
    }

    private void ensureValidPeriod(Instant start, Instant end) {
        if (end.isBefore(start)) {
            throw new BadRequestException(INVALID_SPRINT_PERIOD);
        }
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getKey());
        }
    }
}
