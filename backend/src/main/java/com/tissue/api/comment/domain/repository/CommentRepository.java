package com.tissue.api.comment.domain.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.comment.domain.Comment;
import com.tissue.api.comment.domain.IssueComment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	Optional<IssueComment> findByIdAndIssue_IssueKeyAndIssue_WorkspaceCode(
		Long id,
		String issueKey,
		String workspaceCode
	);
}
