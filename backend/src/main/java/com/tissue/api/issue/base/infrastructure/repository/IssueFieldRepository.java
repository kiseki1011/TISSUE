package com.tissue.api.issue.base.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;

public interface IssueFieldRepository extends JpaRepository<IssueField, Long> {

	Optional<IssueField> findByIssueTypeAndId(IssueType issueType, Long id);

	List<IssueField> findByIssueType(IssueType issueType);

	boolean existsByIssueTypeAndLabel_Normalized(IssueType issueType, String label);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update IssueField f "
		+ "set f.archived = true, "
		+ "f.updatedAt = CURRENT_TIMESTAMP, "
		+ "f.version = f.version + 1 "
		+ "where f.issueType = :issueType "
		+ "and f.archived = false")
	int softDeleteByIssueType(@Param("issueType") IssueType issueType);
}
