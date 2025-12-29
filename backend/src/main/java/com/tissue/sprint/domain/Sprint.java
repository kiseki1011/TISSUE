package com.tissue.sprint.domain;

import static com.tissue.sprint.domain.enums.SprintStatus.*;

import com.tissue.common.entity.BaseEntity;
import com.tissue.project.domain.Project;
import com.tissue.sprint.domain.enums.SprintStatus;
import com.tissue.sprint.domain.exception.SprintExceptions;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.lang.Nullable;

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

    public static Sprint create(@NonNull Project project, @NonNull String title, @Nullable String goal) {
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

    public void start(@NonNull Instant dueAt) {
        if (this.status != PLANNING) {
            throw SprintExceptions.invalidStatusTransition(this.status, PLANNING, ACTIVE);
        }

        this.startedAt = Instant.now();
        ensureValidPeriod(this.startedAt, dueAt);
        this.dueAt = dueAt;
        this.status = ACTIVE;
    }

    public void complete() {
        if (this.status != ACTIVE) {
            throw SprintExceptions.invalidStatusTransition(this.status, ACTIVE, COMPLETED);
        }
        this.status = COMPLETED;
        this.completedAt = Instant.now();
    }

    private void ensureValidPeriod(Instant start, Instant end) {
        if (end.isBefore(start)) {
            throw SprintExceptions.invalidPeriod(start, end);
        }
    }
}
