package com.tissue.feature.agent.model.application.port.usecase;

import com.tissue.feature.agent.model.application.dto.request.CreateAiModelCommand;
import com.tissue.feature.agent.model.application.dto.request.PatchAiModelCommand;
import com.tissue.feature.agent.model.application.dto.response.AiModelResponse;

public interface AiModelUseCase {

    AiModelResponse create(CreateAiModelCommand cmd, Long actorMemberId);

    void update(Long modelId, PatchAiModelCommand cmd, Long actorMemberId);

    void delete(Long modelId, Long actorMemberId);
}
