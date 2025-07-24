package com.tissue.api.issue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.newmodel.WorkflowStep;

public interface WorkflowStepRepository extends JpaRepository<WorkflowStep, Long> {
}
