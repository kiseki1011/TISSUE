package com.tissue.api.workspace.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.workspace.domain.model.Workspace;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

	Optional<Workspace> findByKey(String key);

	@Query("SELECT w FROM Workspace w "
		+ "LEFT JOIN FETCH w.workspaceMembers "
		+ "WHERE w.key = :key")
	Optional<Workspace> findByKeyWithWorkspaceMembers(@Param("key") String key);

	boolean existsByKey(String key);
}
