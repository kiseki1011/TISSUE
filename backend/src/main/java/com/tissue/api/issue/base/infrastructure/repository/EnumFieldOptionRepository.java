package com.tissue.api.issue.base.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.IssueField;
import com.tissue.api.issue.base.domain.model.IssueType;

public interface EnumFieldOptionRepository extends JpaRepository<EnumFieldOption, Long> {

	Optional<EnumFieldOption> findByFieldAndId(IssueField field, Long id);

	List<EnumFieldOption> findByFieldOrderByPositionAsc(IssueField field);

	boolean existsByFieldAndLabel_Normalized(IssueField field, String label);

	int countByField(IssueField field);

	@Query("select count(v) > 0 "
		+ "from IssueFieldValue v "
		+ "where v.enumOption = :option")
	boolean isInUse(@Param("option") EnumFieldOption option);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update EnumFieldOption o "
		+ "set o.archived = true, "
		+ "o.lastModifiedAt = instant, "
		+ "o.version = o.version + 1 "
		+ "where o.field = :field "
		+ "and o.archived = false")
	int softDeleteByField(@Param("field") IssueField field);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update EnumFieldOption o "
		+ "set o.archived = true, "
		+ "o.lastModifiedAt = instant, "
		+ "o.version = o.version + 1 "
		+ "where o.field in "
		+ "(select f from IssueField f "
		+ "where f.issueType = :issueType) "
		+ "and o.archived = false")
	int softDeleteByIssueType(@Param("issueType") IssueType issueType);
}
