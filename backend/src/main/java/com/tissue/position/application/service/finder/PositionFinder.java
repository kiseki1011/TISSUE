package com.tissue.position.application.service.finder;

import com.tissue.position.application.port.out.PositionQueryRepository;
import com.tissue.position.domain.Position;
import com.tissue.position.domain.exception.PositionNotFoundException;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PositionFinder {

    private final PositionQueryRepository positionRepository;

    public Position getBy(Long positionId, Workspace workspace) {
        return positionRepository
                .findByIdAndWorkspace(positionId, workspace)
                .orElseThrow(() -> new PositionNotFoundException(positionId, workspace.getKey()));
    }

    public Position getBy(Long positionId, String workspaceKey) {
        return positionRepository
                .findByIdAndWorkspace_Key(positionId, workspaceKey)
                .orElseThrow(() -> new PositionNotFoundException(positionId, workspaceKey));
    }
}
