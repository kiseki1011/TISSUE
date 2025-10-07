package com.tissue.api.issue.collaborator.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.collaborator.domain.model.IssueAssignee;

public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, Long> {
}
