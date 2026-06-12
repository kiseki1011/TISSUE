package com.tissue.feature.issue.domain.vo;

import com.tissue.feature.project.domain.vo.ProjectKey;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Issue Key VO (Value Object)
 *
 * <p>Must be globally unique. The prefix is a {@link ProjectKey}, so the format will look like
 * {@code {project key}-{issue key number}}.
 * <pre>
 * example: {@code DEMO-123}
 * </pre>
 */
@Getter
@Embeddable
@EqualsAndHashCode
public class IssueKey {

    private static final String SEPARATOR = "-";

    @Column(name = "issue_key", nullable = false, unique = true)
    private String value;

    @SuppressWarnings("NullAway.Init")
    protected IssueKey() {}

    private IssueKey(String value) {
        this.value = value;
    }

    public static IssueKey of(String projectKey, Long number) {
        return new IssueKey(projectKey + SEPARATOR + number);
    }

    public String getProjectKey() {
        int idx = value.lastIndexOf(SEPARATOR);
        return value.substring(0, idx);
    }

    public Long getNumber() {
        int idx = value.lastIndexOf(SEPARATOR);
        return Long.parseLong(value.substring(idx + 1));
    }

    @Override
    public String toString() {
        return value;
    }
}
