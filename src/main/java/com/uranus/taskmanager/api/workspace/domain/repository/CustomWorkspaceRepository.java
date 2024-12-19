package com.uranus.taskmanager.api.workspace.domain.repository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.workspace.domain.Workspace;

public interface CustomWorkspaceRepository {
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	Workspace saveWithNewTransaction(Workspace workspace);
}
