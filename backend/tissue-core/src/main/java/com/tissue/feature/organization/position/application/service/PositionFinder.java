package com.tissue.feature.organization.position.application.service;

import com.tissue.feature.organization.position.application.port.repository.PositionQueryRepository;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.position.domain.exception.PositionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PositionFinder {

    private final PositionQueryRepository positionRepository;

    public Position getBy(String workspaceKey, Long positionId) {
        return positionRepository
                .findByWorkspace_KeyAndId(workspaceKey, positionId)
                .orElseThrow(() -> new PositionNotFoundException(workspaceKey, positionId));
    }

    public Position getWithWorkspaceBy(String workspaceKey, Long positionId) {
        return positionRepository
                .findWithWorkspaceByKeys(workspaceKey, positionId)
                .orElseThrow(() -> new PositionNotFoundException(workspaceKey, positionId));
    }
}
