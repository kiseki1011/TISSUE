package com.tissue.feature.workflow.application.port.repository;

import com.tissue.feature.workflow.domain.WorkflowState;
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
             AND w.id = :workflowId
             AND s.id = :stateId
       """)
    Optional<WorkflowState> findStateWithHierarchyByWorkspaceKeyAndWorkflowIdAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("workflowId") Long workflowId,
            @Param("stateId") Long stateId);
}
