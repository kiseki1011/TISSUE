package com.tissue.feature.workflow.application.port.repository;

import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Native, FK-safe bulk deletes for permanently deleting a whole workflow aggregate.
 *
 * <p>The aggregate has an inter-child foreign key (a transition references its source/target states)
 * and a cycle (workflow.initial_state_id → workflow_state, workflow_state.workflow_id → workflow).
 * Cascading through the JPA mappings lets Hibernate pick a delete order that tries to null the transitions
 * NOT-NULL {@code source_state_id} to break the dependency, which violates the constraint.
 * Deleting each child set explicitly in child-to-parent order, and turning the initial-state link null
 * before the states go, sidesteps that entirely.
 */
@LLMGenerated(
        model = "claude-opus-4-8",
        llmInvolvement = LLMInvolvement.ASSISTED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Test passes. Tested manually from client. Reviewed manually.",
        reviewedBy = "kiseki1011")
public interface WorkflowDeleteRepository extends Repository<Workflow, Long> {

    /**
     * Guards hang off transitions. Must go before the transitions.
     */
    @Modifying(clearAutomatically = true)
    @Query(
            value = "DELETE FROM transition_guard_config "
                    + "WHERE transition_id IN (SELECT id FROM workflow_transition WHERE workflow_id = :workflowId)",
            nativeQuery = true)
    void deleteGuardConfigs(@Param("workflowId") Long workflowId);

    /**
     * Transitions reference the states (source/target). Must go before the states.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM workflow_transition WHERE workflow_id = :workflowId", nativeQuery = true)
    void deleteTransitions(@Param("workflowId") Long workflowId);

    /**
     * Break the workflow → initial-state link so the states can be deleted.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE workflow SET initial_state_id = NULL WHERE id = :workflowId", nativeQuery = true)
    void detachInitialState(@Param("workflowId") Long workflowId);

    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM workflow_state WHERE workflow_id = :workflowId", nativeQuery = true)
    void deleteStates(@Param("workflowId") Long workflowId);

    /**
     * The workflow row itself. Done last.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "DELETE FROM workflow WHERE id = :workflowId", nativeQuery = true)
    void deleteWorkflow(@Param("workflowId") Long workflowId);
}
