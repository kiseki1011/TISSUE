package com.tissue.api.issuetype.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.api.issuetype.domain.EnumFieldOption;
import com.tissue.api.issuetype.domain.IssueField;
import com.tissue.api.issuetype.domain.IssueType;

public interface EnumFieldOptionCommandRepository extends Repository<EnumFieldOption, Long> {

	EnumFieldOption save(EnumFieldOption option);

	List<EnumFieldOption> saveAll(Iterable<EnumFieldOption> options);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update EnumFieldOption o "
		+ "set o.archived = true, "
		+ "o.lastModifiedAt = instant, "
		+ "o.version = o.version + 1 "
		+ "where o.issueField = :issueField "
		+ "and o.archived = false")
	int softDeleteByField(@Param("issueField") IssueField issueField);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update EnumFieldOption o "
		+ "set o.archived = true, "
		+ "o.lastModifiedAt = instant, "
		+ "o.version = o.version + 1 "
		+ "where o.issueField in "
		+ "(select f from IssueField f "
		+ "where f.issueType = :issueType) "
		+ "and o.archived = false")
	int softDeleteByIssueType(@Param("issueType") IssueType issueType);
}
