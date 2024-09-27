package com.uranus.taskmanager.api.repository;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

public interface CustomWorkspaceRepository {
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	Workspace saveWithNewTransaction(Workspace workspace);
}
