package com.tissue.api.issuetype.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tissue.api.issuetype.domain.IssueType;
import com.tissue.api.workspace.domain.model.Workspace;

public interface IssueTypeRepository extends JpaRepository<IssueType, Long> {

	boolean existsByWorkspaceAndLabel_Normalized(Workspace workspace, String label);

	Optional<IssueType> findByWorkspace_KeyAndId(String workspaceKey, Long id);

	Optional<IssueType> findByWorkspaceAndId(Workspace workspace, Long id);
}
