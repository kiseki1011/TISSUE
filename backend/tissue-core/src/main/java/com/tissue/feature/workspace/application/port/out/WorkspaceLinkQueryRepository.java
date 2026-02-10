package com.tissue.feature.workspace.application.port.out;

import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceLinkQueryRepository extends Repository<WorkspaceInviteLink, Long> {

    Optional<WorkspaceInviteLink> findByToken(String token);
}
