package com.tissue.issue.domain.vo;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@Embeddable
@EqualsAndHashCode
public class IssueKey {

    private static final String SEPARATOR = "-";

    @Column(name = "issue_key", nullable = false, unique = true)
    private String value;

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
