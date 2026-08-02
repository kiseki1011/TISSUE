package com.tissue.feature.activitylog.adapter.persistence;

import java.time.Instant;

/**
 * Projection for per-project latest activity aggregate.
 */
public interface ProjectLastActivityRow {

    String getProjectKey();

    Instant getLastActivityAt();
}
