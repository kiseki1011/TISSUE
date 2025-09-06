package com.tissue.api.issue.base.infrastructure.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issue.base.domain.model.IssueType;
import com.tissue.api.workspace.domain.model.Workspace;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {

	boolean existsByWorkspaceAndLabel(Workspace workspace, String label);

	boolean existsByWorkspaceAndLabelAndIdNot(Workspace workspace, String label, Long excludeId);

	Optional<IssueType> findByWorkspace_KeyAndKey(String workspaceKey, String key);

	Optional<IssueType> findByWorkspaceAndKey(Workspace workspace, String key);
}
