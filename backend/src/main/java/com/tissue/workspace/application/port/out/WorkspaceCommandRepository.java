package com.tissue.workspace.application.port.out;

import com.tissue.workspace.domain.Workspace;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceCommandRepository extends Repository<Workspace, Long> {

    Workspace save(Workspace workspace);

    Optional<Workspace> findByKey(String key);
}
