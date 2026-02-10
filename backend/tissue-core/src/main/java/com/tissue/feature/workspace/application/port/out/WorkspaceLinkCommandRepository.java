package com.tissue.feature.workspace.application.port.out;

import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import org.springframework.data.repository.Repository;

public interface WorkspaceLinkCommandRepository extends Repository<WorkspaceInviteLink, Long> {

    WorkspaceInviteLink save(WorkspaceInviteLink inviteLink);
}
