package com.tissue.feature.member.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AgentErrorCode implements ErrorCode {
    AGENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Agent not found"),
    DUPLICATE_AGENT_NAME(HttpStatus.CONFLICT, "An agent with this name already exists"),
    OWNER_MUST_BE_HUMAN(HttpStatus.FORBIDDEN, "Only human members can own agents");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
