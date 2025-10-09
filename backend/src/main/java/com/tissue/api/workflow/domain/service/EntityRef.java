package com.tissue.api.workflow.domain.service;

public record EntityRef(Long id, String tempKey) {
	public EntityRef {
		if ((id == null) == (tempKey == null)) {
			throw new IllegalArgumentException("One of id or tempKey must be provided");
		}
	}

	public boolean isExisting() {
		return id != null;
	}
}
