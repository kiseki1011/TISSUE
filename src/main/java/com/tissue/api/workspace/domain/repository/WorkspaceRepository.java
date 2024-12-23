package com.tissue.api.workspace.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workspace.domain.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long>, CustomWorkspaceRepository {

	Optional<Workspace> findByCode(String code);

	boolean existsByCode(String code);
}
