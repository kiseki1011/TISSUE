package com.tissue.feature.issue.domain;

import com.tissue.feature.issue.domain.enums.PullRequestState;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

/**
 * A pull request linked to an issue, mirroring {@link IssueBranch}: kept as current state rather than as
 * history, so the issue shows what is open right now instead of leaving a reader to reconstruct it from a
 * stream of activity entries.
 */
@Entity
@Getter
@LLMGenerated(llmInvolvement = LLMInvolvement.ASSISTED, model = "claude-opus-5")
@Table(name = "issue_pull_request", uniqueConstraints = @UniqueConstraint(columnNames = {"issue_id", "pr_number"}))
public class IssuePullRequest extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @Column(name = "pr_number", nullable = false)
    private int number;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String url;

    @Nullable
    @Column(name = "author_name")
    private String authorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PullRequestState state;

    @Column(name = "last_event_at", nullable = false)
    private Instant lastEventAt;

    @SuppressWarnings("NullAway.Init")
    protected IssuePullRequest() {}

    public static IssuePullRequest create(
            Issue issue,
            int number,
            String title,
            String url,
            @Nullable String authorName,
            PullRequestState state,
            Instant lastEventAt) {
        IssuePullRequest pullRequest = new IssuePullRequest();
        pullRequest.issue = issue;
        pullRequest.number = number;
        pullRequest.title = title;
        pullRequest.url = url;
        pullRequest.authorName = authorName;
        pullRequest.state = state;
        pullRequest.lastEventAt = lastEventAt;
        return pullRequest;
    }

    public void update(String title, PullRequestState state, @Nullable String authorName, Instant lastEventAt) {
        this.title = title;
        this.state = state;
        this.authorName = authorName;
        this.lastEventAt = lastEventAt;
    }
}
