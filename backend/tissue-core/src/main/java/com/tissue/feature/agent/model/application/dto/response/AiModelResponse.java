package com.tissue.feature.agent.model.application.dto.response;

import com.tissue.feature.agent.model.domain.AiModel;

public record AiModelResponse(Long modelId) {

    public static AiModelResponse from(AiModel model) {
        return new AiModelResponse(model.getId());
    }
}
