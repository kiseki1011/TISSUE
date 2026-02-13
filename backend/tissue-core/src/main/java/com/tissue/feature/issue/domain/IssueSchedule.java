package com.tissue.feature.issue.domain;

import static com.tissue.feature.issue.domain.exception.IssueErrorCode.DUE_DATE_MUST_BE_FUTURE;

import com.tissue.shared.exception.base.BadRequestException;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Embeddable
@Getter
public class IssueSchedule {

    @Nullable
    @Column(name = "started_at")
    private Instant startedAt;

    @Nullable
    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Nullable
    @Column(name = "due_at")
    private Instant dueAt;

    @SuppressWarnings("NullAway.Init")
    protected IssueSchedule() {}

    public static IssueSchedule of(@Nullable Instant dueAt) {
        IssueSchedule schedule = new IssueSchedule();
        schedule.dueAt = ensureValidDueAt(dueAt);

        return schedule;
    }

    void markStarted() {
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    void markResolved() {
        if (this.resolvedAt == null) {
            this.resolvedAt = Instant.now();
        }
    }

    void clearResolved() {
        this.resolvedAt = null;
    }

    void updateDueDate(@Nullable Instant dueAt) {
        this.dueAt = ensureValidDueAt(dueAt);
    }

    private static @Nullable Instant ensureValidDueAt(@Nullable Instant instant) {
        if (instant == null) {
            return null;
        }

        Instant now = Instant.now();
        if (instant.isBefore(now)) {
            throw new BadRequestException(DUE_DATE_MUST_BE_FUTURE);
        }
        return instant;
    }
}
