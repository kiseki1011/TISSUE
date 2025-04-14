package com.tissue.api.common.entity;

import java.time.LocalDateTime;

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
public class WorkspaceBaseEntity extends BaseDateEntity {

	@CreatedBy
	@Column(updatable = false)
	private Long createdByMember;

	@LastModifiedBy
	private Long lastModifiedByWorkspaceMember;

	@Column(nullable = false)
	private boolean deleted = false;

	private LocalDateTime deletedAt;

	public void softDelete() {
		this.deleted = true;
		this.deletedAt = LocalDateTime.now();
	}

	public void restore() {
		if (this.deleted) {
			this.deleted = false;
			this.deletedAt = null;
		}
	}
}
