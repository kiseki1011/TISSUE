package com.tissue.api.issue.base.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;

public interface IssueFieldRepository extends JpaRepository<IssueField, Long> {

	boolean existsByIssueTypeAndLabel(IssueType issueType, String label);

	boolean existsByIssueTypeAndLabelAndIdNot(IssueType type, String label, Long excludeId);

	List<IssueField> findByIssueType(IssueType issueType);

	Optional<IssueField> findByIssueTypeAndKey(IssueType issueType, String key);

	Optional<IssueField> findByKey(String key);
}
