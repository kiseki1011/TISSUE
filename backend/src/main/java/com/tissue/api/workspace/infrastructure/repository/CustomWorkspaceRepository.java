package com.tissue.api.workspace.infrastructure.repository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workspace.domain.model.Workspace;

public interface CustomWorkspaceRepository {
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	Workspace saveWithNewTransaction(Workspace workspace);
}
