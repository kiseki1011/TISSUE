package com.tissue.feature.project.application.port.repository;

/**
 * Projection for per-project active member count.
 */
public interface ProjectMemberCountRow {

    String getProjectKey();

    long getMemberCount();
}
