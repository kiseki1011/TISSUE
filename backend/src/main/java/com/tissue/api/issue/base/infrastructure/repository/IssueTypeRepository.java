package com.tissue.api.issue.base.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.workspace.domain.model.Workspace;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {

	boolean existsByWorkspaceAndLabel_Normalized(Workspace workspace, String label);

	Optional<IssueType> findByWorkspace_KeyAndId(String workspaceKey, Long id);

	Optional<IssueType> findByWorkspaceAndId(Workspace workspace, Long id);
}
