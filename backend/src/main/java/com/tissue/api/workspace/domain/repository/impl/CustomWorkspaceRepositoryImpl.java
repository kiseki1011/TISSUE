package com.tissue.api.workspace.domain.repository.impl;

import org.springframework.stereotype.Repository;

import com.tissue.api.workspace.domain.Workspace;
import com.tissue.api.workspace.domain.repository.CustomWorkspaceRepository;

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
