package com.tissue.api.common.entity;

import java.time.Instant;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity extends BaseDateEntity {

	@CreatedBy
	@Column(updatable = false)
	private Long createdBy;

	@LastModifiedBy
	private Long lastModifiedBy;

	@Column(nullable = false)
	private boolean archived = false;

	private Instant archivedAt;

	public void archive() {
		if (!archived) {
			this.archived = true;
			this.archivedAt = Instant.now();
		}
	}

	/**
	 * Disable if there is no restore policy.
	 */
	public void restore() {
		if (archived) {
			this.archived = false;
			this.archivedAt = null;
		}
	}
}
