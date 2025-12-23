package com.tissue.comment.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tissue.comment.domain.exception.CommentExceptions;
import com.tissue.common.entity.BaseEntity;
import com.tissue.workspace.domain.WorkspaceMember;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "type")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class Comment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AUTHOR_ID", nullable = false)
	private WorkspaceMember author;

	private boolean isEdited = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT_COMMENT_ID")
	private Comment parentComment;

	@OneToMany(mappedBy = "parentComment")
	private final List<Comment> childComments = new ArrayList<>();

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CommentStatus status = CommentStatus.ACTIVE;

	private LocalDateTime deletedAt;
	private Long deletedByMemberId;

	public Comment(String content, WorkspaceMember author, Comment parentComment) {
		this.content = content;
		this.author = author;
		this.parentComment = parentComment;

		if (parentComment != null) {
			validateParentComment();
			parentComment.addChildComment(this);
		}
	}

	public void updateContent(String content) {
		this.content = content;
		this.isEdited = true;
	}

	public void softDelete(Long deletedByMemberId) {
		this.status = CommentStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
		this.deletedByMemberId = deletedByMemberId;
	}

	public void addChildComment(Comment child) {
		child.parentComment = this;
		this.childComments.add(child);
	}

	public void validateCanEdit(WorkspaceMember workspaceMember) {
		if (author.equals(workspaceMember)) {
			return;
		}
		throw CommentExceptions.notAuthor(this.id, workspaceMember.getMemberId());
	}

	protected void validateParentComment() {
		if (parentComment == null) {
			return;
		}
		if (parentComment.getParentComment() != null) {
			throw CommentExceptions.nestedLimitExceeded(parentComment.getId());
		}

		// if (parentComment.getClass() != this.getClass()) {
		// 	throw CommentExceptions.relationMismatch(parentComment.getId(), this.getClass().getSimpleName());
		// }
	}
}
