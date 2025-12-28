package com.tissue.workspace.application.port.out;

import com.tissue.workspace.domain.WorkspaceInviteLink;
import org.springframework.data.repository.Repository;

public interface WorkspaceLinkCommandRepository extends Repository<WorkspaceInviteLink, Long> {

    WorkspaceInviteLink save(WorkspaceInviteLink inviteLink);
}
