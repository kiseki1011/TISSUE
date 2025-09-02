package com.tissue.api.issue.base.infrastructure.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.Issue;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueFieldValue;

public interface IssueFieldValueRepository extends JpaRepository<IssueFieldValue, Long> {

	List<IssueFieldValue> findByIssue(Issue issue);

	boolean existsByField(IssueField field);
}
