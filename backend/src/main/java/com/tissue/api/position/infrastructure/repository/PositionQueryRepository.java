package com.tissue.api.position.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.position.domain.model.Position;

public interface PositionQueryRepository extends JpaRepository<Position, Long> {

	List<Position> findAllByWorkspace_KeyOrderByCreatedDateAsc(String workspaceKey);

	List<Position> findAllByWorkspace_Key(String workspaceKey);
}
