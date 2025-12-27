package com.tissue.workspace.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.workspace.domain.Workspace;

public interface WorkspaceQueryRepository extends Repository<Workspace, Long> {

	Optional<Workspace> findByKey(String key);

	boolean existsByKey(String key);
}
