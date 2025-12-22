package com.tissue.team.application.port.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.team.domain.Team;
import com.tissue.workspace.domain.Workspace;

public interface TeamQueryRepository extends Repository<Team, Long> {

	Optional<Team> findByIdAndWorkspace_Key(Long id, String workspaceKey);

	Optional<Team> findByIdAndWorkspace(Long id, Workspace workspace);

	List<Team> findAllByWorkspace_KeyOrderByCreatedAtAsc(String workspaceKey);

	List<Team> findAllByWorkspace_Key(String workspaceKey);

	boolean existsByWorkspaceAndName_Normalized(Workspace workspace, String name);

	@Query("SELECT COUNT(wmt) > 0 FROM WorkspaceMemberTeam wmt WHERE wmt.team = :team")
	boolean existsByWorkspaceMembers(@Param("team") Team team);
}
