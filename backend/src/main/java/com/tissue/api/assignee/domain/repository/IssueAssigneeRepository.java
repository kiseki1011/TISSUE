package com.tissue.api.assignee.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.assignee.domain.IssueAssignee;

public interface IssueAssigneeRepository extends JpaRepository<IssueAssignee, Long> {
}
