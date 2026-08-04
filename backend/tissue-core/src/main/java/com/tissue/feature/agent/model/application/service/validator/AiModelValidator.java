package com.tissue.feature.agent.model.application.service.validator;

import static com.tissue.feature.agent.model.domain.exception.AiModelErrorCode.DUPLICATE_AI_MODEL_NAME;

import com.tissue.feature.agent.model.application.port.repository.AiModelRepository;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiModelValidator {

    private final AiModelRepository aiModelRepository;

    public void ensureUniqueLabel(Name name) {
        boolean duplicated = aiModelRepository.existsByName_NormalizedName(name.getNormalizedName());
        if (duplicated) {
            throw new ResourceConflictException(DUPLICATE_AI_MODEL_NAME);
        }
    }
}
