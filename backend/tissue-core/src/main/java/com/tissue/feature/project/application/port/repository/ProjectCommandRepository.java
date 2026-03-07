package com.tissue.feature.project.application.port.repository;

import com.tissue.feature.project.domain.Project;
import java.time.Instant;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectCommandRepository extends Repository<Project, Long> {

    Project save(Project project);

    @Modifying
    @Query("UPDATE Project p "
            + "SET p.softDeleted = true, p.softDeletedAt = :now, p.archived = true, p.archivedAt = :now "
            + "WHERE p.workspaceKey = :workspaceKey AND p.softDeleted = false")
    void softDeleteAllByWorkspaceKey(@Param("workspaceKey") String workspaceKey, @Param("now") Instant now);
}
