package com.tissue.feature.issue.domain;

import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.entity.SoftDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

// TODO: HardDeleteEntity를 사용해야 할까?
@Entity
@Getter
public class IssueReviewer extends SoftDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReviewStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", insertable = false, updatable = false)
    private ProjectMember reviewer;

    @SuppressWarnings("NullAway.Init")
    protected IssueReviewer() {}

    public IssueReviewer(ProjectMember reviewer, Issue issue) {
        this.issue = issue;
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
