package com.tissue.api.issue.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.domain.model.IssueFieldValue;
import com.tissue.api.issuetype.domain.IssueField;

public interface IssueFieldValueRepository extends JpaRepository<IssueFieldValue, Long> {

	List<IssueFieldValue> findByIssue(Issue issue);

	boolean existsByField(IssueField field);
}
