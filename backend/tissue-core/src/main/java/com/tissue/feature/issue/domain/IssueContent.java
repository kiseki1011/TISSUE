package com.tissue.feature.issue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Getter
@Embeddable
public class IssueContent {

    @Lob
    @Column(name = "content")
    private String content;

    @Lob
    @Column(name = "summary")
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
