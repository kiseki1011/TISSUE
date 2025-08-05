package com.tissue.api.issue.workflow.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.workflow.domain.model.WorkflowStatus;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStatus, Long> {
}
