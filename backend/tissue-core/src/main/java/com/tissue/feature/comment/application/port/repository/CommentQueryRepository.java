package com.tissue.feature.comment.application.port.repository;

import com.tissue.feature.comment.domain.Comment;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface CommentQueryRepository extends Repository<Comment, Long> {

    @Query("""
                SELECT c FROM Comment c
                JOIN FETCH c.author wm
                JOIN FETCH wm.member m
                WHERE c.workspaceKey = :workspaceKey AND c.issueKey = :issueKey
                ORDER BY c.createdAt ASC
            """)
    List<Comment> findByIssue(@Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey);

    @Query(value = """
                SELECT c FROM Comment c
                JOIN FETCH c.author wm
                JOIN FETCH wm.member m
                WHERE c.workspaceKey = :workspaceKey
                  AND c.issueKey = :issueKey
                  AND c.parentComment IS NULL
            """, countQuery = """
                SELECT COUNT(c) FROM Comment c
                WHERE c.workspaceKey = :workspaceKey
                  AND c.issueKey = :issueKey
                  AND c.parentComment IS NULL
            """)
    Page<Comment> findRootsByIssue(
            @Param("workspaceKey") String workspaceKey, @Param("issueKey") String issueKey, Pageable pageable);

    @Query("""
                SELECT c FROM Comment c
                JOIN FETCH c.author wm
                JOIN FETCH wm.member m
                WHERE c.parentComment.id IN :parentIds
                ORDER BY c.createdAt ASC
            """)
    List<Comment> findRepliesByParentIds(@Param("parentIds") Collection<Long> parentIds);

    @Query(value = """
                SELECT c FROM Comment c
                JOIN FETCH c.author wm
                JOIN FETCH wm.member m
                JOIN FETCH c.issue i
                WHERE c.workspaceKey = :workspaceKey AND c.createdBy = :memberId AND c.softDeleted = false
                ORDER BY c.createdAt DESC
            """, countQuery = """
                SELECT COUNT(c) FROM Comment c
                WHERE c.workspaceKey = :workspaceKey AND c.createdBy = :memberId AND c.softDeleted = false
            """)
    Page<Comment> findAllByWorkspaceKeyAndMemberId(
            @Param("workspaceKey") String workspaceKey, @Param("memberId") Long memberId, Pageable pageable);
}
