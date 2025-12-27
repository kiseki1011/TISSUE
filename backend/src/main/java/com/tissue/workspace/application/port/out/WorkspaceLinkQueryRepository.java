package com.tissue.workspace.application.port.out;

import com.tissue.workspace.domain.WorkspaceInviteLink;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceLinkQueryRepository extends Repository<WorkspaceInviteLink, Long> {

    Optional<WorkspaceInviteLink> findByToken(String token);
}
