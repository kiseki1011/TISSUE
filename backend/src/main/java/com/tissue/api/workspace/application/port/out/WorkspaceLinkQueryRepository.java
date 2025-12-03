package com.tissue.api.workspace.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.api.workspace.domain.WorkspaceInviteLink;

public interface WorkspaceLinkQueryRepository extends Repository<WorkspaceInviteLink, Long> {

	Optional<WorkspaceInviteLink> findByToken(String token);
}
