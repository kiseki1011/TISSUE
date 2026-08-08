package com.tissue.feature.agent.model.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiModelErrorCode implements ErrorCode {
    AI_MODEL_NOT_FOUND(HttpStatus.NOT_FOUND, "AI model not found"),
    DUPLICATE_AI_MODEL_NAME(HttpStatus.CONFLICT, "AI model name already exists");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
