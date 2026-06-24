package com.tissue.feature.issue.application.port.repository;

import com.tissue.feature.issue.application.dto.response.MyReviewStatusView;
import com.tissue.feature.issue.domain.IssueReviewer;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface IssueReviewerQueryRepository extends Repository<IssueReviewer, Long> {

    @Query("""
                SELECT r
                FROM IssueReviewer r
                JOIN FETCH r.reviewer pm
                JOIN FETCH pm.member m
                JOIN r.issue i
                WHERE i.key.value = :issueKey
            """)
    List<IssueReviewer> findByIssueKey(@Param("issueKey") String issueKey);

    /**
     * The given member's review status on each of the given issues (only issues they actually review
     * are returned). One query for a whole page of results, keyed by issue id, so a search can show
     * "my review status" without N lookups.
     */
    @Query("""
                SELECT new com.tissue.feature.issue.application.dto.response.MyReviewStatusView(
                    r.issue.id, r.status)
                FROM IssueReviewer r
                WHERE r.reviewer.member.id = :memberId
                  AND r.issue.id IN :issueIds
            """)
    List<MyReviewStatusView> findMyReviewStatuses(
            @Param("memberId") Long memberId, @Param("issueIds") Set<Long> issueIds);
}
