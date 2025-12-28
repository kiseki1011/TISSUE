package com.tissue.workflow.application.port.out;

import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

    Optional<WorkflowState> findByIdAndWorkflow(Long id, Workflow workflow);
}
