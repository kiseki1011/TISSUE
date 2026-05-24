package com.tissue.feature.issuetype.application.port.repository;

import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import java.util.List;
import java.util.Optional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueFieldRepository extends Repository<IssueField, Long> {

    IssueField save(IssueField issueField);

    void delete(IssueField issueField);

    @Cacheable(value = "issueFields", cacheManager = "localCacheManager", key = "#issueType.id")
    List<IssueField> findByIssueType(IssueType issueType);

    @Query("""
       SELECT f
       FROM IssueField f
       JOIN FETCH f.issueType t
       JOIN FETCH t.project p
       WHERE f.id = :fieldId
         AND p.key = :projectKey
         AND p.workspaceKey = :workspaceKey
   """)
    Optional<IssueField> findWithProjectAndIssueTypeByWorkspaceKeyAndProjectKeyAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("projectKey") String projectKey,
            @Param("fieldId") Long fieldId);

    boolean existsByIssueTypeAndName_NormalizedName(IssueType issueType, String label);

    @Query("""
       SELECT f
       FROM IssueField f
       LEFT JOIN FETCH f.options o
       WHERE f.issueType.id = :issueTypeId
       ORDER BY f.position ASC
   """)
    List<IssueField> findAllWithOptionsByIssueTypeId(@Param("issueTypeId") Long issueTypeId);
}
