package com.tissue.issuetype.application.port.out;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.issuetype.domain.EnumFieldOption;
import com.tissue.issuetype.domain.IssueField;

public interface EnumFieldOptionQueryRepository extends Repository<EnumFieldOption, Long> {

	Optional<EnumFieldOption> findByIdAndIssueField(Long id, IssueField field);

	List<EnumFieldOption> findByIssueFieldOrderByPositionAsc(IssueField field);

	boolean existsByIssueFieldAndName_Normalized(IssueField field, String label);

	int countByIssueField(IssueField field);

	@Query("select count(v) > 0 "
		+ "from IssueFieldValue v "
		+ "where v.enumOption = :option")
	boolean isInUse(@Param("option") EnumFieldOption option);
}
