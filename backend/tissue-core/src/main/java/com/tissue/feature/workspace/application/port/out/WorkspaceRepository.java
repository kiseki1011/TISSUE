package com.tissue.feature.workspace.application.port.out;

import com.tissue.feature.workspace.domain.Workspace;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface WorkspaceRepository extends Repository<Workspace, Long> {

    Workspace save(Workspace workspace);

    Optional<Workspace> findByKey(String key);

    boolean existsByKey(String key);
}
