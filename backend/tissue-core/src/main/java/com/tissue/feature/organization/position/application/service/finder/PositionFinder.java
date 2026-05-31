package com.tissue.feature.organization.position.application.service.finder;

import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.organization.position.domain.exception.PositionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionFinder {

    private final PositionRepository positionRepository;

    public Position getById(Long positionId) {
        return positionRepository.findById(positionId).orElseThrow(() -> new PositionNotFoundException(positionId));
    }
}
