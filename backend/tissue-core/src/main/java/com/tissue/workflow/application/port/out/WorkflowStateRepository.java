package com.tissue.workflow.application.port.out;

import com.tissue.workflow.domain.WorkflowState;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkflowStateRepository extends Repository<WorkflowState, Long> {

    @Query("""
           SELECT s
           FROM WorkflowState s
           JOIN FETCH s.workflow w
           JOIN FETCH w.project p
           WHERE p.workspaceKey = :workspaceKey
             AND p.key = :projectKey
             AND w.id = :workflowId
             AND s.id = :stateId
       """)
    Optional<WorkflowState> findStateWithHierarchyByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("workflowId") Long workflowId,
            @Param("stateId") Long stateId);
}
