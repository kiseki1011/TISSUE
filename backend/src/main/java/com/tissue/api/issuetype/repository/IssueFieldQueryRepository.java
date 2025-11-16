package com.tissue.api.issuetype.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;

public interface IssueFieldQueryRepository extends Repository<IssueField, Long> {

	Optional<IssueField> findById(Long id);

	Optional<IssueField> findByIdAndIssueType(Long id, IssueType issueType);

	List<IssueField> findByIssueType(IssueType issueType);

	boolean existsByIssueTypeAndLabel_Normalized(IssueType issueType, String label);
}
