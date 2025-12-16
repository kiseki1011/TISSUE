package com.tissue.position.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.position.domain.model.Position;

public interface PositionQueryRepository extends JpaRepository<Position, Long> {

	List<Position> findAllByWorkspace_KeyOrderByCreatedAtAsc(String workspaceKey);

	List<Position> findAllByWorkspace_Key(String workspaceKey);
}
