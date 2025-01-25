package com.tissue.api.comment.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tissue.api.comment.domain.enums.CommentStatus;
import com.tissue.api.common.entity.WorkspaceContextBaseEntity;
import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.workspacemember.domain.WorkspaceMember;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

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
public class Comment extends WorkspaceContextBaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "AUTHOR_ID", nullable = false)
	private WorkspaceMember author;

	// 대댓글 구조를 위한 self-referencing 관계
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "PARENT_COMMENT_ID")
	private Comment parentComment;

	@OneToMany(mappedBy = "parentComment")
	private final List<Comment> childComments = new ArrayList<>();

	// 댓글의 상태 관리
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private CommentStatus status = CommentStatus.ACTIVE;

	// soft delete를 위한 필드들
	private LocalDateTime deletedAt;
	private Long deletedBy;

	public void delete(Long deletedBy) {
		this.status = CommentStatus.DELETED;
		this.deletedAt = LocalDateTime.now();
		this.deletedBy = deletedBy;
	}

	public boolean isAuthoredBy(WorkspaceMember member) {
		return author.equals(member);
	}

	// 바로 검증하는 것 고려
	public boolean canEdit(WorkspaceMember member) {
		return author.equals(member) || member.roleIsHigherThan(WorkspaceRole.MANAGER);
	}

	// 대댓글 추가 시 1-depth 제한과 타입 검증
	protected void validateParentComment() {
		if (parentComment == null) {
			return;
		}

		if (parentComment.getParentComment() != null) {
			throw new InvalidOperationException("Comments can only be nested one level deep.");
		}

		if (parentComment.getClass() != this.getClass()) {
			throw new InvalidOperationException(
				String.format("Parent comment type(%s) and child comment type(%s) must match.",
					parentComment.getClass().getSimpleName(),
					this.getClass().getSimpleName())
			);
		}
	}

	public void addChildComment(Comment child) {
		child.validateParentComment();
		child.parentComment = this;
		this.childComments.add(child);
	}
}
