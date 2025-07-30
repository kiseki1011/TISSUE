package com.tissue.api.issue.base.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueFieldValue;

public interface IssueFieldValueRepository extends JpaRepository<IssueFieldValue, Long> {
}
