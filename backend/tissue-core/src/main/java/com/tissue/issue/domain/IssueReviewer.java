package com.tissue.issue.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.issue.domain.enums.ReviewStatus;
import com.tissue.project.domain.ProjectMember;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class IssueReviewer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
