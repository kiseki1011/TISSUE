package com.tissue.feature.organization.position.application.service.validator;

import static com.tissue.feature.organization.position.domain.exception.PositionErrorCode.DUPLICATE_POSITION_NAME;

import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionValidator {

    private final PositionRepository positionRepository;

    public void ensureUniqueLabel(Name name) {
        boolean duplicated = positionRepository.existsByName_NormalizedName(name.getNormalizedName());
        if (duplicated) {
            throw new ResourceConflictException(DUPLICATE_POSITION_NAME);
        }
    }
}
