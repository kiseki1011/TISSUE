package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueFieldQueryRepository extends Repository<IssueField, Long> {

    List<IssueField> findByIssueType(IssueType issueType);

    @Query("""
       SELECT f
       FROM IssueField f
       JOIN FETCH f.issueType t
       JOIN FETCH t.project p
       WHERE f.id = :fieldId
         AND t.id = :issueTypeId
         AND p.key = :projectKey
         AND p.workspaceKey = :workspaceKey
   """)
    Optional<IssueField> findWithProjectAndIssueTypeByKeys(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("issueTypeId") Long issueTypeId,
            @Param("fieldId") Long fieldId);

    boolean existsByIssueTypeAndName_Normalized(IssueType issueType, String label);
}
