package com.tissue.feature.agent.model.application.service;

import com.tissue.feature.agent.model.application.dto.request.CreateAiModelCommand;
import com.tissue.feature.agent.model.application.dto.request.PatchAiModelCommand;
import com.tissue.feature.agent.model.application.dto.response.AiModelResponse;
import com.tissue.feature.agent.model.application.port.repository.AiModelRepository;
import com.tissue.feature.agent.model.application.port.usecase.AiModelUseCase;
import com.tissue.feature.agent.model.application.service.finder.AiModelFinder;
import com.tissue.feature.agent.model.application.service.validator.AiModelValidator;
import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.Patchers;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AiModelService implements AiModelUseCase {

    private final AiModelFinder aiModelFinder;
    private final AiModelRepository aiModelRepository;
    private final AiModelValidator aiModelValidator;
    private final MemberCommandRepository memberCommandRepository;

    @Override
    public AiModelResponse create(CreateAiModelCommand cmd, Long actorMemberId) {
        aiModelValidator.ensureUniqueLabel(cmd.name());

        AiModel model = AiModel.create(cmd.name(), cmd.description(), cmd.color());

        AiModel saved = aiModelRepository.save(model);

        return AiModelResponse.from(saved);
    }

    @Override
    public void update(Long modelId, PatchAiModelCommand cmd, Long actorMemberId) {
        AiModel model = aiModelFinder.getById(modelId);

        Patchers.apply(cmd.name(), newName -> {
            Name name = Name.of(newName);
            if (!isNameUnchanged(model, name)) {
                aiModelValidator.ensureUniqueLabel(name);
                model.rename(name);
            }
        });
        Patchers.apply(cmd.description(), model::updateDescription);
        Patchers.apply(cmd.color(), model::updateColor);
    }

    @Override
    public void delete(Long modelId, Long actorMemberId) {
        AiModel model = aiModelFinder.getById(modelId);

        memberCommandRepository.clearModelAssignments(model);

        aiModelRepository.delete(model);
    }

    private boolean isNameUnchanged(AiModel model, Name newName) {
        return Objects.equals(model.getName(), newName.toString());
    }
}
