package com.tissue.api.comment.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.comment.domain.model.Comment;
import com.tissue.api.comment.domain.model.IssueComment;

public interface CommentRepository extends JpaRepository<Comment, Long> {

	Optional<IssueComment> findByIdAndIssue_KeyAndIssue_Workspace_Key(
		Long id,
		String issueKey,
		String workspaceKey
	);
}
