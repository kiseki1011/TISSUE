package com.tissue.feature.project.application.port.out;

import com.tissue.feature.project.domain.Project;
import org.springframework.data.repository.Repository;

public interface ProjectCommandRepository extends Repository<Project, Long> {

    Project save(Project project);
}
