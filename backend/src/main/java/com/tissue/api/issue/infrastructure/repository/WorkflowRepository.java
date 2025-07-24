package com.tissue.api.issue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.newmodel.WorkflowDefinition;

public interface WorkflowRepository extends JpaRepository<WorkflowDefinition, Long> {
}
