package com.tissue.api.workspace.domain.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.workspace.domain.Workspace;

public interface WorkspaceCommandRepository extends Repository<Workspace, Long> {

	Workspace save(Workspace workspace);

	Optional<Workspace> findByKey(String key);
}
