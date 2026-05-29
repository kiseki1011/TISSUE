package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.IssueAttachment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueAttachmentRepository extends Repository<IssueAttachment, Long> {

    IssueAttachment save(IssueAttachment attachment);

    void delete(IssueAttachment attachment);

    @Query("""
            SELECT a
            FROM IssueAttachment a
            JOIN FETCH a.issue i
            JOIN FETCH i.project p
            WHERE i.key.value = :issueKey
              AND a.id = :attachmentId
            """)
    Optional<IssueAttachment> findWithIssueAndProjectByIssueKeyAndId(
            @Param("issueKey") String issueKey, @Param("attachmentId") Long attachmentId);

    long countByIssueKey(String issueKey);

    @Query("""
            SELECT a FROM IssueAttachment a
            WHERE a.issueKey = :issueKey
            ORDER BY a.createdAt ASC
            """)
    List<IssueAttachment> findByIssueKey(@Param("issueKey") String issueKey);
}
