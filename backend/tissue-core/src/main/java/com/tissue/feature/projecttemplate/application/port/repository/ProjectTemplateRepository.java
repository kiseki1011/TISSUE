package com.tissue.feature.projecttemplate.application.port.repository;

import com.tissue.feature.projecttemplate.domain.ProjectTemplate;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ProjectTemplateRepository extends Repository<ProjectTemplate, Long> {

    ProjectTemplate save(ProjectTemplate projectTemplate);

    Optional<ProjectTemplate> findById(Long id);

    @Query("SELECT t FROM ProjectTemplate t WHERE t.id = :id AND t.workspace.key = :workspaceKey")
    Optional<ProjectTemplate> findByIdAndWorkspaceKey(@Param("id") Long id, @Param("workspaceKey") String workspaceKey);

    void delete(ProjectTemplate projectTemplate);
}
