package com.tissue.api.issue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.IssueAssignee;

public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, Long> {
}
