package com.tissue.api.workflow.application.port.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.workflow.domain.Workflow;

public interface WorkflowRepository extends JpaRepository<Workflow, Long> {
}
