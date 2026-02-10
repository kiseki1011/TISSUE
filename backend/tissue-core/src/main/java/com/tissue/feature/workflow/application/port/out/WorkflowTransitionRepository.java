package com.tissue.feature.workflow.application.port.out;

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
           JOIN FETCH w.project p
           WHERE p.workspaceKey = :workspaceKey
             AND p.key = :projectKey
             AND w.id = :workflowId
             AND t.id = :transitionId
       """)
    Optional<WorkflowTransition> findTransitionWithHierarchyByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("workflowId") Long workflowId,
            @Param("transitionId") Long transitionId);
}
