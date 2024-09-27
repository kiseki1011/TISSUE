package com.uranus.taskmanager.api.repository;

import org.springframework.stereotype.Repository;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class CustomWorkspaceRepositoryImpl implements CustomWorkspaceRepository {

	private final EntityManager entityManager;

	@Override
	public Workspace saveWithNewTransaction(Workspace workspace) {
		entityManager.persist(workspace);
		return workspace;
	}
}
