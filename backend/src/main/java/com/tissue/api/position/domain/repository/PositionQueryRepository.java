package com.tissue.api.position.domain.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.position.domain.Position;

public interface PositionQueryRepository extends JpaRepository<Position, Long> {

	List<Position> findAllByWorkspaceCodeOrderByCreatedDateAsc(String workspaceCode);
}
