package com.tissue.position.application.service.validator;

import com.tissue.common.vo.Name;
import com.tissue.position.application.port.out.PositionQueryRepository;
import com.tissue.position.domain.Position;
import com.tissue.position.domain.exception.DuplicatePositionNameException;
import com.tissue.position.domain.exception.PositionInUseException;
import com.tissue.workspace.domain.Workspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionValidator {

    private final PositionQueryRepository positionQueryRepository;

    public void ensureUniqueName(Workspace workspace, String name) {
        String normalizedName = Name.of(name).getNormalized();

        if (positionQueryRepository.existsByWorkspaceAndName_Normalized(workspace, normalizedName)) {
            throw new DuplicatePositionNameException(name, workspace.getKey());
        }
    }

    public void ensureDeletable(Position position) {
        if (positionQueryRepository.existsByWorkspaceMembers(position)) {
            throw new PositionInUseException(position);
        }
    }
}
