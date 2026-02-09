package com.tissue.organization.team.application.port.out;

import com.tissue.organization.team.domain.Team;
import com.tissue.workspace.domain.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TeamQueryRepository extends Repository<Team, Long> {

    Optional<Team> findByWorkspace_KeyAndId(String workspaceKey, Long id);

    Optional<Team> findByWorkspaceAndId(Workspace workspace, Long id);

    List<Team> findAllByWorkspace_KeyOrderByCreatedAtAsc(String workspaceKey);

    List<Team> findAllByWorkspace_Key(String workspaceKey);

    @Query("SELECT t FROM Team t " + "JOIN FETCH t.workspace w " + "WHERE w.key = :workspaceKey " + "AND t.id = :id")
    Optional<Team> findWithWorkspaceByKeys(@Param("workspaceKey") String workspaceKey, @Param("id") Long id);

    boolean existsByWorkspaceAndName_Normalized(Workspace workspace, String name);

    @Query("SELECT COUNT(wmt) > 0 FROM WorkspaceMemberTeam wmt WHERE wmt.team = :team")
    boolean existsByWorkspaceMembers(@Param("team") Team team);
}
