package com.tissue.api.common.entity;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.tissue.api.global.key.KeyPrefixPolicy;

import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class PrefixedKeyEntity extends BaseEntity {

	public abstract Long getId();

	public abstract String getKey();

	protected abstract void setKey(String key);

	protected abstract String keyPrefix();

	@PrePersist
	protected void assignKey() {
		if (getKey() == null && getId() != null) {
			setKey(KeyPrefixPolicy.format(keyPrefix(), getId()));
		}
	}
}
