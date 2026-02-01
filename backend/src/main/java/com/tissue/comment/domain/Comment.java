package com.tissue.comment.domain;

import com.tissue.comment.domain.exception.NestedCommentLimitExceededException;
import com.tissue.global.entity.BaseEntity;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    public static Comment create(WorkspaceMember author, Issue issue, String content,
        @Nullable Comment parentComment) {
        Comment comment = new Comment();
        comment.author = author;
        comment.issue = issue;
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
            throw new NestedCommentLimitExceededException(parent.getId());
        }

        if (!parent.getIssue().equals(this.issue)) {
            throw new IllegalArgumentException("Parent comment must belong to the same issue");
        }
    }
}
