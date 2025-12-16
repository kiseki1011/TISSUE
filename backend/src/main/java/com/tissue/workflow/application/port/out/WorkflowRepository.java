package com.tissue.workflow.application.port.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.workflow.domain.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
}
