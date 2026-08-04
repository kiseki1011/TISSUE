package com.tissue.feature.agent.model.application.service.finder;

import com.tissue.feature.agent.model.application.port.repository.AiModelRepository;
import com.tissue.feature.agent.model.domain.AiModel;
import com.tissue.feature.agent.model.domain.exception.AiModelNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiModelFinder {

    private final AiModelRepository aiModelRepository;

    public AiModel getById(Long modelId) {
        return aiModelRepository.findById(modelId).orElseThrow(() -> new AiModelNotFoundException(modelId));
    }
}
