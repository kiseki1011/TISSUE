package com.uranus.taskmanager.api.position.domain.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.uranus.taskmanager.api.position.domain.Position;

public interface PositionRepository extends JpaRepository<Position, Long> {
	Optional<Position> findByIdAndWorkspaceCode(Long id, String workspaceCode);

	List<Position> findAllByWorkspaceCodeOrderByCreatedDateAsc(String workspaceCode);

	@Query("SELECT COUNT(wm) > 0 FROM WorkspaceMember wm WHERE wm.position = :position")
	boolean existsByWorkspaceMembers(@Param("position") Position position);
}
