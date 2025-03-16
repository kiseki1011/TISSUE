package com.tissue.api.workspace.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workspace.domain.Workspace;

public interface WorkspaceQueryRepository extends JpaRepository<Workspace, Long> {

	Optional<Workspace> findByCode(String code);
}
