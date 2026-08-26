package com.tissue.feature.issue.application.dto.request;

import java.util.EnumSet;
import java.util.Set;

/**
 * A section of the aggregated issue detail view a caller wants assembled.
 *
 * <p>Each section costs its own query, so a caller that will not render one can leave it out rather than
 * pay for it. An omitted section comes back empty rather than absent, so the response shape never varies
 * with what was asked for.
 */
public enum IssueDetailSection {
    TRANSITIONS,
    HIERARCHY,
    RELATIONS,
    COMMENTS,
    VCS;

    public static Set<IssueDetailSection> all() {
        return EnumSet.allOf(IssueDetailSection.class);
    }
}
