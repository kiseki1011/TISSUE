package com.tissue.api.team.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.team.domain.model.Team;

public interface TeamQueryRepository extends JpaRepository<Team, Long> {

	List<Team> findAllByWorkspace_KeyOrderByCreatedDateAsc(String workspaceKey);

	List<Team> findAllByWorkspace_Key(String workspaceKey);
}
