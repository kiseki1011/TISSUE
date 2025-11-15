package com.tissue.api.issue.domain;

import java.time.Instant;

import org.springframework.lang.Nullable;

import com.tissue.api.issue.exception.InvalidDueDateException;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IssueSchedule {

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "resolved_at")
	private Instant resolvedAt;

	@Column(name = "due_at")
	private Instant dueAt;

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

	private static Instant ensureValidDueAt(Instant instant) {
		if (instant == null) {
			return null;
		}

		Instant now = Instant.now();
		if (instant.isBefore(now)) {
			throw new InvalidDueDateException(instant, now);
		}

		return instant;
	}
}
