package com.tissue.workflow.application.port.out;

import com.tissue.workflow.domain.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {}
