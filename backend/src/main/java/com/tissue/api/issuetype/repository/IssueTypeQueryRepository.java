package com.tissue.api.issuetype.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.project.domain.Project;

public interface IssueTypeQueryRepository extends JpaRepository<IssueType, Long> {

	boolean existsByLabel_NormalizedAndProject(String label, Project project);

	Optional<IssueType> findByIdAndProjectKeyAndWorkspaceKey(Long id, String projectKey, String workspaceKey);

	Optional<IssueType> findByIdAndProject(Long id, Project project);
}
