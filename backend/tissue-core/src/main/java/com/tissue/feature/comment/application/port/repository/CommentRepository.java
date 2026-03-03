package com.tissue.feature.comment.application.port.repository;

import com.tissue.feature.comment.domain.Comment;
import com.tissue.feature.issue.domain.Issue;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends Repository<Comment, Long> {

    Comment save(Comment comment);

    Optional<Comment> findByIdAndIssue_Key(Long id, String issueKey);

    Optional<Comment> findByIssueAndId(Issue issue, Long commentId);

    @Query("""
       SELECT c
       FROM Comment c
       JOIN FETCH c.issue i
       JOIN FETCH i.project p
       WHERE p.workspaceKey = :workspaceKey
         AND i.key.value = :issueKey
         AND c.id = :commentId
   """)
    Optional<Comment> findWithProjectAndIssueByKeysAndId(
            @Param("workspaceKey") String workspaceKey,
            @Param("issueKey") String issueKey,
            @Param("commentId") Long commentId);
}
