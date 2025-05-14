package com.tissue.api.workspace.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workspace.domain.model.Workspace;

public interface WorkspaceQueryRepository extends JpaRepository<Workspace, Long> {

	Optional<Workspace> findByCode(String code);
}
