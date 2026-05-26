package com.tissue.feature.issue.domain;

import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(
        indexes = {
            @Index(name = "idx_issue_reviewer_issue_id", columnList = "issue_id"),
            @Index(name = "idx_issue_reviewer_reviewer_id", columnList = "reviewer_id")
        })
@Getter
public class IssueReviewer extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(name = "issue_key", nullable = false, updatable = false)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private ProjectMember reviewer;

    @SuppressWarnings("NullAway.Init")
    protected IssueReviewer() {}

    public IssueReviewer(ProjectMember reviewer, Issue issue) {
        this.issue = issue;
        this.workspaceKey = issue.getWorkspaceKey();
        this.issueKey = issue.getKey();
        this.reviewer = reviewer;
        this.status = ReviewStatus.PENDING;
    }

    public void approve() {
        issue.ensureEditable();
        this.status = ReviewStatus.APPROVED;
    }

    public void reject() {
        issue.ensureEditable();
        this.status = ReviewStatus.CHANGES_REQUESTED;
    }

    public void resetReview() {
        this.status = ReviewStatus.PENDING;
    }
}
