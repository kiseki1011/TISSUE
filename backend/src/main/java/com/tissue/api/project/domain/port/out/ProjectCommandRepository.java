package com.tissue.api.project.domain.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.api.project.domain.Project;

public interface ProjectCommandRepository extends Repository<Project, Long> {

	Project save(Project project);
}
