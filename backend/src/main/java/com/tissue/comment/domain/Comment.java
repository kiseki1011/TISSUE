package com.tissue.comment.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.lang.Nullable;

import com.tissue.comment.domain.exception.CommentExceptions;
import com.tissue.common.entity.BaseEntity;
import com.tissue.issue.domain.Issue;
import com.tissue.workspace.domain.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// TODO: should i use soft-delete?
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private WorkspaceMember author;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "issue_id", nullable = false)
	private Issue issue;

	@Column(nullable = false)
	private boolean isEdited;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "parent_comment_id")
	private Comment parentComment;

	@OneToMany(mappedBy = "parentComment")
	private final List<Comment> childComments = new ArrayList<>();

	// @Enumerated(EnumType.STRING)
	// @Column(nullable = false)
	// private CommentStatus status = CommentStatus.ACTIVE;

	public static Comment create(
		@NonNull String content,
		@NonNull WorkspaceMember author,
		@Nullable Comment parentComment
	) {
		Comment comment = new Comment();
		comment.content = content;
		comment.author = author;
		comment.parentComment = parentComment;
		comment.isEdited = false;

		if (parentComment != null) {
			comment.validateParentComment();
			parentComment.addChildComment(comment);
		}

		return comment;
	}

	public void updateContent(String content) {
		this.content = content;
		this.isEdited = true;
	}

	public void addChildComment(@NonNull Comment child) {
		child.parentComment = this;
		this.childComments.add(child);
	}

	protected void validateParentComment() {
		if (parentComment == null) {
			return;
		}
		if (parentComment.getParentComment() != null) {
			throw CommentExceptions.nestedLimitExceeded(parentComment.getId());
		}
	}
}
