package com.tissue.api.issue.base.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueType;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {
}
