package com.tissue.feature.organization.team.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements ErrorCode {
    TEAM_NOT_FOUND(HttpStatus.NOT_FOUND, "Team not found"),
    DUPLICATE_TEAM_NAME(HttpStatus.CONFLICT, "Team name already exists");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
