package com.tissue.feature.workspace.application.port.repository;

import com.tissue.feature.workspace.domain.Workspace;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends Repository<Workspace, Long> {

    Workspace save(Workspace workspace);

    Optional<Workspace> findByKey(String key);

    boolean existsByKey(String key);

    long countBySoftDeletedFalse();

    Optional<Workspace> findFirstBySoftDeletedFalseOrderByIdAsc();

    @Query(value = """
            SELECT w.*
            FROM workspace w
            WHERE w.workspace_key = :workspaceKey
              AND w.soft_deleted = true
            """, nativeQuery = true)
    Optional<Workspace> findDeletedByKey(@Param("workspaceKey") String workspaceKey);

    @Query(value = """
            SELECT w.*
            FROM workspace w
            JOIN workspace_member wm ON wm.workspace_id = w.id
            WHERE wm.member_id = :memberId
              AND wm.workspace_role = 'OWNER'
              AND wm.soft_deleted = false
              AND w.soft_deleted = true
            """, nativeQuery = true)
    List<Workspace> findDeletedWorkspacesByOwnerMemberId(@Param("memberId") Long memberId);
}
