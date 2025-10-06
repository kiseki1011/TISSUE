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
public abstract class NoArchiveEntity extends BaseDateEntity {

	@CreatedBy
	@Column(updatable = false)
	private Long createdBy;

	@LastModifiedBy
	private Long lastModifiedBy;

	public abstract Long getId();

	private static Class<?> effectiveClass(Object o) {
		if (o instanceof org.hibernate.proxy.HibernateProxy p) {
			return p.getHibernateLazyInitializer().getPersistentClass();
		}
		return o.getClass();
	}

	@Override
	public final boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (effectiveClass(this) != effectiveClass(o))
			return false;
		BaseEntity that = (BaseEntity)o;
		return getId() != null && java.util.Objects.equals(getId(), that.getId());
	}

	@Override
	public final int hashCode() {
		return effectiveClass(this).hashCode();
	}
}
