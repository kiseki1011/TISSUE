package com.tissue.api.issuetype.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.workspace.domain.Workspace;

public interface IssueTypeQueryRepository extends JpaRepository<IssueType, Long> {

	boolean existsByLabel_NormalizedAndWorkspace(String label, Workspace workspace);

	Optional<IssueType> findByIdAndWorkspace_Key(Long id, String workspaceKey);

	Optional<IssueType> findByIdAndWorkspace(Long id, Workspace workspace);
}
