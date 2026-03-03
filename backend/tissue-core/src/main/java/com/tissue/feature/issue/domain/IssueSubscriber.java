package com.tissue.feature.issue.domain;

import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;

@Entity
@Getter
public class IssueSubscriber extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(insertable = false, updatable = false)
    private ProjectMember subscriber;

    @Column(nullable = false)
    private LocalDateTime subscribedAt;

    @SuppressWarnings("NullAway.Init")
    protected IssueSubscriber() {}

    public IssueSubscriber(ProjectMember subscriber, Issue issue) {
        this.issue = issue;
        this.subscriber = subscriber;
        this.subscribedAt = LocalDateTime.now();
    }
}
