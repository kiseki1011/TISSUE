package com.tissue.api.issuetype.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;

public interface IssueFieldCommandRepository extends Repository<IssueField, Long> {

	IssueField save(IssueField issueField);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update IssueField f "
		+ "set f.archived = true, "
		+ "f.lastModifiedAt = instant, "
		+ "f.version = f.version + 1 "
		+ "where f.issueType = :issueType "
		+ "and f.archived = false")
	int softDeleteByIssueType(@Param("issueType") IssueType issueType);
}
