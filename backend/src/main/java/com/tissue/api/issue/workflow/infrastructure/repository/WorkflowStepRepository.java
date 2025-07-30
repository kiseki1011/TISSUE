package com.tissue.api.issue.workflow.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.WorkflowStep;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
}
