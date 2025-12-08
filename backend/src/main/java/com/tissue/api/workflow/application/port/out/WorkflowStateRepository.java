package com.tissue.api.workflow.application.port.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;

public interface WorkflowStateRepository extends JpaRepository<WorkflowState, Long> {

	Optional<WorkflowState> findByIdAndWorkflow(Long id, Workflow workflow);
}
