package com.tissue.feature.workflow.application.port.repository;

import com.tissue.feature.workflow.domain.WorkflowTransition;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkflowTransitionRepository extends Repository<WorkflowTransition, Long> {

    @Query("""
           SELECT t
           FROM WorkflowTransition t
           JOIN FETCH t.workflow w
           WHERE w.id = :workflowId
             AND t.id = :transitionId
       """)
    Optional<WorkflowTransition> findTransitionWithHierarchyByWorkflowIdAndId(
            @Param("workflowId") Long workflowId, @Param("transitionId") Long transitionId);
}
