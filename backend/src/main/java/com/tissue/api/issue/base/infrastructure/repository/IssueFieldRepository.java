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

	// TODO: IssueField @Version 적용 후, 배치 업데이트 메서드에 o.version=o.version+1 추가
	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update IssueField f "
		+ "set f.archived = true, "
		+ "f.updatedAt = CURRENT_TIMESTAMP "
		+ "where f.issueType = :issueType "
		+ "and f.archived = false")
	int softDeleteByIssueType(@Param("issueType") IssueType issueType);
}
