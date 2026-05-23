package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.IssueReviewer;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueReviewerQueryRepository extends Repository<IssueReviewer, Long> {

    @Query("""
                SELECT r
                FROM IssueReviewer r
                JOIN FETCH r.reviewer pm
                JOIN FETCH pm.workspaceMember wm
                JOIN FETCH wm.member m
                JOIN r.issue i
                WHERE i.workspaceKey = :workspaceKey
                  AND i.key.value = :issueKey
            """)
    List<IssueReviewer> findByIssue(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);
}
