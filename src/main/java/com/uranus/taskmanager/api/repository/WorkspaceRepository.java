package com.uranus.taskmanager.api.repository;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
}
