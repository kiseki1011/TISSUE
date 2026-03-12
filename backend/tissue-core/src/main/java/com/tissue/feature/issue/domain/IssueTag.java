package com.tissue.feature.issue.domain;

import com.tissue.feature.tag.domain.Tag;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;

@Entity
@Getter
public class IssueTag extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Column(name = "issue_key", nullable = false, updatable = false)
    private String issueKey;

    @SuppressWarnings("NullAway.Init")
    protected IssueTag() {}

    public IssueTag(Issue issue, Tag tag) {
        this.issue = issue;
        this.tag = tag;
        this.workspaceKey = issue.getWorkspaceKey();
        this.issueKey = issue.getKey();
    }
}
