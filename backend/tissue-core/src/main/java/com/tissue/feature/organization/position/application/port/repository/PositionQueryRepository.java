package com.tissue.feature.organization.position.application.port.repository;

import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.workspace.domain.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PositionQueryRepository extends Repository<Position, Long> {

    Optional<Position> findByWorkspace_KeyAndId(String workspaceKey, Long id);

    @Query("SELECT p FROM Position p "
            + "JOIN FETCH p.workspace w "
            + "WHERE w.key = :workspaceKey "
            + "AND p.id = :id")
    Optional<Position> findWithWorkspaceByKeys(@Param("workspaceKey") String workspaceKey, @Param("id") Long id);

    List<Position> findAllByWorkspace_KeyOrderByCreatedAtAsc(String workspaceKey);

    boolean existsByWorkspaceAndName_NormalizedName(Workspace workspace, String name);

    @Query("SELECT COUNT(wmp) > 0 FROM WorkspaceMemberPosition wmp WHERE wmp.position = :position")
    boolean existsByWorkspaceMembers(@Param("position") Position position);
}
