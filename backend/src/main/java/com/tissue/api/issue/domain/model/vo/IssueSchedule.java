package com.tissue.api.issue.domain.model.vo;

import static com.tissue.api.common.util.DomainPreconditions.*;

import java.time.Instant;

import org.springframework.lang.Nullable;

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

	public IssueSchedule(@Nullable Instant dueAt) {
		this.dueAt = requireFutureOrPresent(dueAt);
	}

	public void markStarted() {
		if (this.startedAt == null) {
			this.startedAt = Instant.now();
		}
	}

	public void markResolved() {
		if (this.resolvedAt == null) {
			this.resolvedAt = Instant.now();
		}
	}

	public void clearResolved() {
		this.resolvedAt = null;
	}

	public void updateDueDate(@Nullable Instant dueAt) {
		this.dueAt = requireFutureOrPresent(dueAt);
	}
}
