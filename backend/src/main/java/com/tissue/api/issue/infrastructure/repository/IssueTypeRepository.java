package com.tissue.api.issue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;

public interface IssueTypeRepository extends JpaRepository<IssueTypeDefinition, Long> {
}
