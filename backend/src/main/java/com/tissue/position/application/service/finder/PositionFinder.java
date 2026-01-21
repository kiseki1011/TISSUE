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

    // TODO: 어차피 내부적으로는 workspaceId를 사용할 수 있을텐데, 그냥 positionId + workspaceId 조회로 변경할까?
    public Position getBy(Long positionId, String workspaceKey) {
        return positionRepository
                .findByIdAndWorkspace_Key(positionId, workspaceKey)
                .orElseThrow(() -> new PositionNotFoundException(positionId, workspaceKey));
    }
}
