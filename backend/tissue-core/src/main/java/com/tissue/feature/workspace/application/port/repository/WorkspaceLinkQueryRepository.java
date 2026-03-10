package com.tissue.feature.workspace.application.port.repository;

import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceLinkQueryRepository extends Repository<WorkspaceInviteLink, Long> {

    Optional<WorkspaceInviteLink> findByToken(String token);

    List<WorkspaceInviteLink> findAllByWorkspaceKey(String workspaceKey);
}
