package com.tissue.issue.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Lob;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Embeddable
@Getter
public class IssueContent {

    @Nullable
    @Lob
    @Column(name = "content")
    private String content;

    // TODO: should i consider removing this field? i was going to use it for a AI summary feature, but im not sure.
    @Nullable
    @Lob
    @Column(name = "summary")
    private String summary;

    @SuppressWarnings("NullAway.Init")
    protected IssueContent() {}

    public static IssueContent of(@Nullable String content, @Nullable String summary) {
        IssueContent issueContent = new IssueContent();
        issueContent.content = content;
        issueContent.summary = summary;

        return issueContent;
    }

    void updateContent(@Nullable String content) {
        this.content = content;
    }

    void updateSummary(@Nullable String summary) {
        this.summary = summary;
    }
}
