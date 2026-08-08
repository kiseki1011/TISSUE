package com.tissue.feature.agent.model.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.MODEL_ID;

import com.tissue.shared.exception.base.ResourceNotFoundException;

public class AiModelNotFoundException extends ResourceNotFoundException {

    public AiModelNotFoundException(Long modelId) {
        super(AiModelErrorCode.AI_MODEL_NOT_FOUND);
        addContext(MODEL_ID, modelId);
    }
}
