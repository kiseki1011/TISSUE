package com.tissue.feature.organization.position.application.service;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.PatchPositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionResponse;
import com.tissue.feature.organization.position.application.port.repository.PositionRepository;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.organization.position.application.service.finder.PositionFinder;
import com.tissue.feature.organization.position.application.service.validator.PositionValidator;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PositionService implements PositionUseCase {

    private final PositionFinder positionFinder;
    private final PositionRepository positionRepository;
    private final PositionValidator positionValidator;
    private final MemberCommandRepository memberCommandRepository;

    @Override
    public PositionResponse create(CreatePositionCommand cmd, Long actorMemberId) {
        positionValidator.ensureUniqueLabel(cmd.name());

        Position position = Position.create(cmd.name(), cmd.description(), cmd.color());

        Position saved = positionRepository.save(position);

        return PositionResponse.from(saved);
    }

    @Override
    public void update(Long positionId, PatchPositionCommand cmd, Long actorMemberId) {
        Position position = positionFinder.getById(positionId);

        Patchers.apply(cmd.name(), newName -> {
            Name name = Name.of(newName);
            if (!isNameUnchanged(position, name)) {
                positionValidator.ensureUniqueLabel(name);
                position.rename(name);
            }
        });
        Patchers.apply(cmd.description(), position::updateDescription);
        Patchers.apply(cmd.color(), position::updateColor);
    }

    @Override
    public void delete(Long positionId, Long actorMemberId) {
        Position position = positionFinder.getById(positionId);

        memberCommandRepository.clearPositionAssignments(position);

        positionRepository.delete(position);
    }

    private boolean isNameUnchanged(Position position, Name newName) {
        return Objects.equals(position.getName(), newName.toString());
    }
}
