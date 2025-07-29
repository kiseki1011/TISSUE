package com.tissue.api.issue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.newmodel.IssueFieldValue;

public interface IssueFieldValueRepository extends JpaRepository<IssueFieldValue, Long> {
}
