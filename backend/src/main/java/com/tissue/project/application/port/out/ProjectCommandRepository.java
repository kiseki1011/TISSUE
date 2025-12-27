package com.tissue.project.application.port.out;

import com.tissue.project.domain.Project;
import org.springframework.data.repository.Repository;

public interface ProjectCommandRepository extends Repository<Project, Long> {

    Project save(Project project);
}
