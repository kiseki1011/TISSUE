package com.tissue.issuetype.application.port.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.issuetype.domain.IssueField;
import com.tissue.issuetype.domain.IssueType;

public interface IssueFieldQueryRepository extends Repository<IssueField, Long> {

	Optional<IssueField> findById(Long id);

	Optional<IssueField> findByIdAndIssueType(Long id, IssueType issueType);

	List<IssueField> findByIssueType(IssueType issueType);

	boolean existsByIssueTypeAndLabel_Normalized(IssueType issueType, String label);
}
