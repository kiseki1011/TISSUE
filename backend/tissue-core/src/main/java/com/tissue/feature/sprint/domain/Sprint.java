package com.tissue.feature.sprint.domain;

import static com.tissue.feature.sprint.domain.SprintStatus.ACTIVE;
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
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@SQLRestriction("soft_deleted = false")
public class Sprint extends SoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

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
        sprint.ensureEditable();
        sprint.projectKey = project.getKey();
        sprint.title = title;
        sprint.goal = goal;
        sprint.status = PLANNING;

        return sprint;
    }

    public boolean isCompleted() {
        return status == COMPLETED;
    }

    public void updateTitle(String title) {
        ensureEditable();
        this.title = title;
    }

    public void updateGoal(@Nullable String goal) {
        ensureEditable();
        this.goal = goal;
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

    private void ensureValidPeriod(Instant start, Instant end) {
        if (end.isBefore(start)) {
            throw new BadRequestException(INVALID_SPRINT_PERIOD);
        }
    }

    public void ensureEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
