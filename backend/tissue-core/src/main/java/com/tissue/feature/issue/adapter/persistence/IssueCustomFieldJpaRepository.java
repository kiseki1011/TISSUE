package com.tissue.feature.issue.adapter.persistence;

import com.tissue.feature.issue.domain.Issue;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueCustomFieldJpaRepository extends Repository<Issue, Long> {

    @Query(
            value = "SELECT EXISTS(SELECT 1 FROM issue "
                    + "WHERE jsonb_exists(custom_fields, :fieldIdStr) "
                    + "AND soft_deleted = false)",
            nativeQuery = true)
    boolean existsWithCustomField(@Param("fieldIdStr") String fieldIdStr);

    @Query(value = """
            SELECT EXISTS(
                SELECT 1 FROM issue
                WHERE soft_deleted = false
                AND (
                    custom_fields->>CAST(:fieldIdStr AS text) = CAST(:optionIdStr AS text)
                    OR jsonb_exists(custom_fields->CAST(:fieldIdStr AS text), CAST(:optionIdStr AS text))
                )
            )
            """, nativeQuery = true)
    boolean isOptionInUse(@Param("fieldIdStr") String fieldIdStr, @Param("optionIdStr") String optionIdStr);
}
