package com.tissue.api.common.entity;

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
public class WorkspaceContextBaseEntity extends BaseDateEntity {

	@CreatedBy
	@Column(updatable = false)
	private Long createdByWorkspaceMember;

	@LastModifiedBy
	private Long lastModifiedByWorkspaceMember;

	public void setCreatedByWorkspaceMember(Long createdByWorkspaceMember) {
		this.createdByWorkspaceMember = createdByWorkspaceMember;
	}
}
