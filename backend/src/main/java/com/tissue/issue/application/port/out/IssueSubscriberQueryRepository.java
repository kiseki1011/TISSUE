package com.tissue.issue.application.port.out;

import com.tissue.issue.domain.IssueSubscriber;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueSubscriberQueryRepository extends Repository<IssueSubscriber, Long> {

    /** Get the list of subsribers for a specific issue. */
    @Query("""
                SELECT s
                FROM IssueSubscriber s
                JOIN FETCH s.subscriber pm
                JOIN FETCH pm.workspaceMember wm
                JOIN FETCH wm.member m
                JOIN FETCH s.issue i
                JOIN FETCH i.project p
                JOIN FETCH p.workspace w
                WHERE w.key = :workspaceKey
                  AND i.key.value = :issueKey
            """)
    List<IssueSubscriber> findByIssue(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    /** Get the number of subsribers for a specific issue. */
    @Query("""
                SELECT COUNT(s)
                FROM IssueSubscriber s
                JOIN s.issue i
                JOIN i.project p
                JOIN p.workspace w
                WHERE w.key = :workspaceKey
                  AND i.key.value = :issueKey
            """)
    int countByIssue(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    /** Check if a specific member subscribes a specific issue. */
    @Query("""
                SELECT COUNT(s) > 0
                FROM IssueSubscriber s
                JOIN s.issue i
                JOIN i.project p
                JOIN p.workspace w
                WHERE w.key = :workspaceKey
                  AND i.key.value = :issueKey
                  AND s.subscriber.memberId = :memberId
            """)
    boolean existsByIssueAndMember(
            @Param("workspaceKey") String workspaceKey,
            @Param("issueKey") String issueKey,
            @Param("memberId") Long memberId);
}
