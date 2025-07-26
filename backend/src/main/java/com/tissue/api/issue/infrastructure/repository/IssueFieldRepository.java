package com.tissue.api.issue.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.newmodel.IssueFieldDefinition;
import com.tissue.api.issue.domain.newmodel.IssueTypeDefinition;

public interface IssueFieldRepository extends JpaRepository<IssueFieldDefinition, Long> {

	// TODO: Is it ok to use a Entity type for searching? Or should i use the id or key?
	//  I want to know exactly how JpaRepository works.
	boolean existsByIssueTypeAndLabel(IssueTypeDefinition issueType, String label);
}
