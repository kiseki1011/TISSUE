package com.tissue.api.position.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.position.domain.model.Position;

public interface PositionRepository extends JpaRepository<Position, Long> {
	Optional<Position> findByIdAndWorkspaceCode(Long id, String workspaceCode);

	@Query("SELECT COUNT(wmp) > 0 FROM WorkspaceMemberPosition wmp WHERE wmp.position = :position")
	boolean existsByWorkspaceMembers(@Param("position") Position position);
}
