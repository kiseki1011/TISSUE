package com.tissue.feature.comment.domain;

import static com.tissue.feature.comment.domain.exception.CommentErrorCode.COMMENT_PARENT_ISSUE_MISMATCH;
import static com.tissue.feature.comment.domain.exception.CommentErrorCode.NESTED_COMMENT_LIMIT_EXCEEDED;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.shared.entity.SoftDeleteEntity;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class Comment extends SoftDeleteEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "issue_key", nullable = false, updatable = false)
    private String issueKey;

    @Column(nullable = false)
    private boolean isEdited;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @OneToMany(mappedBy = "parentComment")
    private final List<Comment> childComments = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected Comment() {}

    public static Comment create(Member author, Issue issue, String content, @Nullable Comment parentComment) {
        Comment comment = new Comment();
        comment.author = author;
        comment.issue = issue;
        comment.issueKey = issue.getKey();
        comment.content = content;
        comment.isEdited = false;
        comment.ensureEditable();

        if (parentComment != null) {
            comment.setParentComment(parentComment);
        }

        return comment;
    }

    public void updateContent(String content) {
        ensureEditable();
        this.content = content;
        this.isEdited = true;
    }

    public boolean isAuthor(Long memberId) {
        return Objects.equals(getCreatedBy(), memberId);
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
            throw new ResourceConflictException(NESTED_COMMENT_LIMIT_EXCEEDED);
        }
        if (!Objects.equals(parent.getIssue(), this.issue)) {
            throw new BadRequestException(COMMENT_PARENT_ISSUE_MISMATCH);
        }
    }

    public void ensureEditable() {
        Project project = issue.getProject();
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getKey());
        }
    }
}
