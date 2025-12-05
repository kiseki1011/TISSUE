package com.tissue.api.issuetype.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.domain.Project;

public interface IssueTypeQueryRepository extends Repository<IssueType, Long> {

	boolean existsByLabel_NormalizedAndProject(String label, Project project);

	Optional<IssueType> findByIdAndProjectKeyAndWorkspaceKey(Long id, String projectKey, String workspaceKey);

	Optional<IssueType> findByIdAndProject(Long id, Project project);
}
