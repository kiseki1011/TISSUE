package com.tissue.api.workspace.domain.port.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workspace.domain.Workspace;

public interface WorkspaceQueryRepository extends JpaRepository<Workspace, Long> {

	Optional<Workspace> findByKey(String key);
}
