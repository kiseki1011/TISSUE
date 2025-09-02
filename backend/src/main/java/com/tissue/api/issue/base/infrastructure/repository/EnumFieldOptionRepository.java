package com.tissue.api.issue.base.infrastructure.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issue.base.domain.model.EnumFieldOption;
import com.tissue.api.issue.base.domain.model.IssueField;

public interface EnumFieldOptionRepository extends JpaRepository<EnumFieldOption, Long> {

	Optional<EnumFieldOption> findByFieldAndKey(IssueField field, String key);

	Optional<EnumFieldOption> findByFieldAndLabel(IssueField field, String label);

	List<EnumFieldOption> findByFieldOrderByPositionAsc(IssueField field);

	boolean existsByFieldAndLabel(IssueField field, String label);

	int countByField(IssueField field);

	@Query("select count(v) > 0 "
		+ "from IssueFieldValue v "
		+ "where v.enumOption = :option")
	boolean isInUse(@Param("option") EnumFieldOption option);
}
