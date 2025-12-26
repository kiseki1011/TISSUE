package com.tissue.comment.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("softDeleted = false")
public class Comment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, columnDefinition = "TEXT")
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

	public static Comment create(
		@NonNull Issue issue,
		@NonNull WorkspaceMember author,
		@NonNull String content,
		@Nullable Comment parentComment
	) {
		Comment comment = new Comment();
		comment.issue = issue;
		comment.author = author;
		comment.content = content;
		comment.isEdited = false;

		if (parentComment != null) {
			comment.setParentComment(parentComment);
		}

		return comment;
	}

	public void updateContent(String content) {
		this.content = content;
		this.isEdited = true;
	}

	public boolean isAuthor(Long memberId) {
		return getCreatedBy().equals(memberId);
	}

	public List<Comment> getChildComments() {
		return Collections.unmodifiableList(childComments);
	}

	private void setParentComment(Comment parentComment) {
		validateParentComment(parentComment);
		this.parentComment = parentComment;
		parentComment.childComments.add(this);
	}

	private void validateParentComment(Comment parent) {
		if (parent.getParentComment() != null) {
			throw CommentExceptions.nestedLimitExceeded(parent.getId());
		}

		if (!parent.getIssue().equals(this.issue)) {
			throw new IllegalArgumentException("Parent comment must belong to the same issue");
		}
	}
}
