package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.ProjectRole;

/**
 * Projection for actor's role in a project.
 */
public interface ProjectMemberRoleRow {

    String getProjectKey();

    ProjectRole getRole();
}
