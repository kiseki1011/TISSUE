package com.tissue.workflow.application.port.out;

import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowTransition;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionRepository extends JpaRepository<WorkflowTransition, Long> {

    Optional<WorkflowTransition> findByIdAndWorkflow(Long id, Workflow workflow);
}
