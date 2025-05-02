package com.tissue.api.issue.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.IssueRelation;

public interface IssueRelationRepository extends JpaRepository<IssueRelation, Long> {
}
