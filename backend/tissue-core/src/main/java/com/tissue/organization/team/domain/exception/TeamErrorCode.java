package com.tissue.organization.team.domain.exception;

import com.tissue.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TeamErrorCode implements ErrorCode {
    TEAM_NOT_FOUND("Team not found"),
    DUPLICATE_TEAM_NAME("Team name must be unique for workspace"),
    TEAM_IN_USE("Team is in use by a workspace member");

    private final String defaultMessage;
}
