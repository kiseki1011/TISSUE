package com.tissue.api.workspace.application.port.out;

import org.springframework.data.repository.Repository;

import com.tissue.api.workspace.domain.WorkspaceInviteLink;

public interface WorkspaceLinkCommandRepository extends Repository<WorkspaceInviteLink, Long> {

	WorkspaceInviteLink save(WorkspaceInviteLink inviteLink);
}
