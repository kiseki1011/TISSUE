package com.tissue.feature.workflow.application.port.repository;

import com.tissue.feature.workflow.domain.WorkflowState;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkflowStateRepository extends Repository<WorkflowState, Long> {

    // workflowId/stateId are globally unique.
    @Query("""
           SELECT s
           FROM WorkflowState s
           JOIN FETCH s.workflow w
           WHERE w.id = :workflowId
             AND s.id = :stateId
       """)
    Optional<WorkflowState> findStateWithHierarchyByWorkflowIdAndId(
            @Param("workflowId") Long workflowId, @Param("stateId") Long stateId);
}
