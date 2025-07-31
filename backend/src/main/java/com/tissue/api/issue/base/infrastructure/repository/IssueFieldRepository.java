package com.tissue.api.issue.base.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueFieldDefinition;
import com.tissue.api.issue.base.domain.model.IssueTypeDefinition;

public interface IssueFieldRepository extends JpaRepository<IssueFieldDefinition, Long> {

	// TODO: Is it ok to use a Entity type for searching? Or should i use the id or key?
	//  I want to know exactly how JpaRepository works.
	boolean existsByIssueTypeAndLabel(IssueTypeDefinition issueType, String label);

	List<IssueFieldDefinition> findByIssueType(IssueTypeDefinition issueType);
}
