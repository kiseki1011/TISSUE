package com.tissue.feature.organization.position.application.service;

import com.tissue.feature.organization.position.application.port.out.PositionQueryRepository;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.position.domain.exception.DuplicatePositionNameException;
import com.tissue.feature.organization.position.domain.exception.PositionInUseException;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.vo.Name;
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
