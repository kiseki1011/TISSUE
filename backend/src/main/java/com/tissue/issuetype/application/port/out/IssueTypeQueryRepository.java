package com.tissue.issuetype.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.issuetype.domain.IssueType;
import com.tissue.project.domain.Project;

public interface IssueTypeQueryRepository extends Repository<IssueType, Long> {

	boolean existsByLabel_NormalizedAndProject(String label, Project project);

	Optional<IssueType> findByIdAndProjectKey(Long id, String projectKey);

	Optional<IssueType> findByIdAndProject(Long id, Project project);
}
