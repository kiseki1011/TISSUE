package com.tissue.position.application.port.out;

import com.tissue.position.domain.Position;
import com.tissue.workspace.domain.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface PositionQueryRepository extends Repository<Position, Long> {

    Optional<Position> findByIdAndWorkspace_Key(Long id, String workspaceKey);

    Optional<Position> findByIdAndWorkspace(Long id, Workspace workspace);

    // TODO: workspaceKey가 아닌 workspaceId 사용을 고려할까?
    List<Position> findAllByWorkspace_KeyOrderByCreatedAtAsc(String workspaceKey);

    boolean existsByWorkspaceAndName_Normalized(Workspace workspace, String name);

    @Query("SELECT COUNT(wmp) > 0 FROM WorkspaceMemberPosition wmp WHERE wmp.position = :position")
    boolean existsByWorkspaceMembers(@Param("position") Position position);
}
