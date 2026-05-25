package com.tissue.feature.issue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
@Embeddable
public class IssueContent {

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @SuppressWarnings("NullAway.Init")
    protected IssueContent() {}

    public static IssueContent of(@Nullable String content, @Nullable String summary) {
        IssueContent issueContent = new IssueContent();
        issueContent.content = Objects.requireNonNullElse(content, "");
        issueContent.summary = Objects.requireNonNullElse(summary, "");

        return issueContent;
    }

    void updateContent(@Nullable String content) {
        this.content = Objects.requireNonNullElse(content, "");
    }

    void updateSummary(@Nullable String summary) {
        this.summary = Objects.requireNonNullElse(summary, "");
    }
}
