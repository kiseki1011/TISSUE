package com.tissue.comment.application.port.out;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import com.tissue.comment.domain.Comment;

public interface CommentQueryRepository extends Repository<Comment, Long> {

	@Query("""
		    SELECT c
		    FROM Comment c
		    JOIN FETCH c.author wm
		    JOIN FETCH wm.member m
		    WHERE c.issue.projectKey = :projectKey
		      AND c.issue.key = :issueKey
		      AND c.deleted = false
		    ORDER BY c.createdAt ASC
		""")
	List<Comment> findByIssue(
		@Param("projectKey") String projectKey,
		@Param("issueKey") String issueKey
	);

	@Query("""
		    SELECT c
		    FROM Comment c
		    JOIN FETCH c.author wm
		    JOIN FETCH wm.member m
		    JOIN FETCH c.issue i
		    WHERE c.createdBy = :memberId
		      AND c.deleted = false
		    ORDER BY c.createdAt DESC
		""")
	List<Comment> findByAuthor(@Param("memberId") Long memberId);
}
