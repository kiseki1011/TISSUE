package com.tissue.feature.workspace.application.port.repository;

/**
 * Projection for counting active workspace members per workspace key.
 *
 * <p>Returned by batch count queries used to enrich workspace summaries
 * without N+1 queries.
 */
public interface WorkspaceMemberCount {
    String getWorkspaceKey();

    long getCount();
}
