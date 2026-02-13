package com.tissue.feature.organization.position.application.service;

import static com.tissue.feature.organization.position.domain.exception.PositionErrorCode.DUPLICATE_POSITION_NAME;
import static com.tissue.feature.organization.position.domain.exception.PositionErrorCode.POSITION_IN_USE;

import com.tissue.feature.organization.position.application.port.repository.PositionQueryRepository;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
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
            throw new ResourceConflictException(DUPLICATE_POSITION_NAME);
        }
    }

    public void ensureDeletable(Position position) {
        if (positionQueryRepository.existsByWorkspaceMembers(position)) {
            throw new BadRequestException(POSITION_IN_USE);
        }
    }
}
