package com.tissue.feature.organization.position.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.organization.position.application.dto.response.PositionSummary;
import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.feature.organization.position.application.port.usecase.PositionQueryUseCase;
import com.tissue.feature.organization.position.domain.Position;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PositionQueryService implements PositionQueryUseCase {

    private final PositionRepository positionRepository;
    private final MemberFinder memberFinder;

    @Override
    public List<PositionSummary> getPositions(Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        List<Position> positions = positionRepository.findAllOrderById();

        return positions.stream().map(PositionSummary::from).toList();
    }
}
