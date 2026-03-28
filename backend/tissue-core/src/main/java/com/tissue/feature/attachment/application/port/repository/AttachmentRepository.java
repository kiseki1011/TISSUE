package com.tissue.feature.attachment.application.port.repository;

import com.tissue.feature.attachment.domain.IssueAttachment;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AttachmentRepository extends Repository<IssueAttachment, Long> {

    IssueAttachment save(IssueAttachment attachment);

    void delete(IssueAttachment attachment);

    @Query("""
            SELECT a
            FROM IssueAttachment a
            JOIN FETCH a.issue i
            JOIN FETCH i.project p
            WHERE p.workspaceKey = :workspaceKey
              AND i.key.value = :issueKey
              AND a.id = :attachmentId
            """)
    Optional<IssueAttachment> findWithIssueAndProjectByKeysAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("issueKey") String issueKey,
            @Param("attachmentId") Long attachmentId);

    long countByIssueKeyAndWorkspaceKey(String issueKey, String workspaceKey);
}
