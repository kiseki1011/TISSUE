package com.tissue.feature.attachment.application.port.repository;

import com.tissue.feature.attachment.domain.IssueAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AttachmentQueryRepository extends Repository<IssueAttachment, Long> {

    @Query("""
            SELECT a FROM IssueAttachment a
            WHERE a.workspaceKey = :workspaceKey AND a.issueKey = :issueKey
            ORDER BY a.createdAt ASC
            """)
    List<IssueAttachment> findByIssue(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);
}
