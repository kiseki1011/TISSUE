package com.tissue.project.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.project.domain.Project;

public interface ProjectCommandRepository extends Repository<Project, Long> {

	Project save(Project project);
}
