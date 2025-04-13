package com.tissue.api.team.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.team.domain.Team;

public interface TeamRepository extends JpaRepository<Team, Long> {
	Optional<Team> findByIdAndWorkspaceCode(Long id, String workspaceCode);

	@Query("SELECT COUNT(wmt) > 0 FROM WorkspaceMemberTeam wmt WHERE wmt.team = :team")
	boolean existsByWorkspaceMembers(@Param("team") Team team);
}
