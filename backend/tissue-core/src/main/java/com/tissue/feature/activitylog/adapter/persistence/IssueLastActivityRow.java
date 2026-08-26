package com.tissue.feature.activitylog.adapter.persistence;

import java.time.Instant;

/**
 * Projection for per-issue latest activity aggregate.
 */
public interface IssueLastActivityRow {

    String getIssueKey();

    Instant getLastActivityAt();
}
