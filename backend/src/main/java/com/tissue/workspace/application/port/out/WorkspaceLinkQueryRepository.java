package com.tissue.workspace.application.port.out;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import com.tissue.workspace.domain.WorkspaceInviteLink;

public interface WorkspaceLinkQueryRepository extends Repository<WorkspaceInviteLink, Long> {

	Optional<WorkspaceInviteLink> findByToken(String token);
}
