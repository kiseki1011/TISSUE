package com.uranus.taskmanager.api.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.uranus.taskmanager.api.domain.workspace.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long>, CustomWorkspaceRepository {

	Optional<Workspace> findByWorkspaceCode(String workspaceCode);

	boolean existsByWorkspaceCode(String workspaceCode);
}
