package com.tissue.comment.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.comment.domain.Comment;
import com.tissue.comment.domain.IssueComment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	Optional<IssueComment> findByIdAndIssue_KeyAndIssue_WorkspaceKey(
		Long id,
		String issueKey,
		String workspaceKey
	);
}
