package com.tissue.comment.application.port.out;

import com.tissue.comment.domain.Comment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CommentQueryRepository extends Repository<Comment, Long> {

    @Query(
            """
                SELECT c
                FROM Comment c
                JOIN FETCH c.author wm
                JOIN FETCH wm.member m
                WHERE c.issue.projectKey = :projectKey
                  AND c.issue.key = :issueKey
                ORDER BY c.createdAt ASC
            """)
    List<Comment> findByIssue(
            @Param("projectKey") String projectKey, @Param("issueKey") String issueKey);

    @Query(
            value =
                    """
                        SELECT c
                        FROM Comment c
                        JOIN FETCH c.author wm
                        JOIN FETCH wm.member m
                        JOIN FETCH c.issue i
                        WHERE c.createdBy = :memberId
                          AND c.softDeleted = false
                        ORDER BY c.createdAt DESC
                    """,
            countQuery =
                    """
                        SELECT COUNT(c)
                        FROM Comment c
                        WHERE c.createdBy = :memberId
                          AND c.softDeleted = false
                    """)
    Page<Comment> findByAuthor(@Param("memberId") Long memberId, Pageable pageable);
}
