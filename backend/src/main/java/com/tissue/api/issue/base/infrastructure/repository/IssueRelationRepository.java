package com.tissue.api.issue.base.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueRelation;

public interface IssueRelationRepository extends JpaRepository<IssueRelation, Long> {
}
