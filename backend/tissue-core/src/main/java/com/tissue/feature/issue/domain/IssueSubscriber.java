package com.tissue.feature.issue.domain;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;

@Entity
@Table(
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_issue_subscriber",
                    columnNames = {"issue_id", "subscriber_id"})
        })
@Getter
public class IssueSubscriber extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "issue_key", nullable = false, updatable = false)
    private String issueKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id", nullable = false)
    private ProjectMember subscriber;

    @Column(nullable = false)
    private Instant subscribedAt;

    @SuppressWarnings("NullAway.Init")
    protected IssueSubscriber() {}

    public IssueSubscriber(ProjectMember subscriber, Issue issue) {
        this.issue = issue;
        this.issueKey = issue.getKey();
        this.subscriber = subscriber;
        this.subscribedAt = Instant.now();
    }
}
