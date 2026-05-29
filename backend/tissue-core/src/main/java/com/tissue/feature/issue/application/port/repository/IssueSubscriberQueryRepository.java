package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.domain.IssueSubscriber;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueSubscriberQueryRepository extends Repository<IssueSubscriber, Long> {

    /**
     * Get the list of subsribers for a specific issue.
     */
    @Query("""
                SELECT s
                FROM IssueSubscriber s
                JOIN FETCH s.subscriber pm
                JOIN FETCH pm.member m
                WHERE s.issueKey = :issueKey
            """)
    List<IssueSubscriber> findByIssueKey(@Param("issueKey") String issueKey);
}
