package com.tissue.feature.member.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    ACTIVE_MEMBER_NOT_FOUND("Active member not found"),
    DUPLICATE_EMAIL("This email is already in use"),
    DUPLICATE_USERNAME("This username is already in use"),
    WORKSPACE_OWNAGE_LIMIT_EXCEEDED("Workspace ownage limit exceeded"),
    WORKSPACE_JOIN_LIMIT_EXCEEDED("Workspace join limit exceeded"),
    INVALID_USERNAME_FORMAT("Invalid username format. Must be 3-22 characters using lowercase letters and numbers."),
    INVALID_NAME_FORMAT("Invalid name format. Must be 3-35 characters without special characters.");

    private final String defaultMessage;
}
