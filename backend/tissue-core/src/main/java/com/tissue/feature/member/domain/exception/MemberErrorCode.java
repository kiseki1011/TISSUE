package com.tissue.feature.member.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Member not found"),
    MEMBER_DELETED(HttpStatus.NOT_FOUND, "Member account has been deleted"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "This email is already in use"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "This username is already in use"),
    WORKSPACE_OWNAGE_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Workspace ownage limit exceeded"),
    WORKSPACE_JOIN_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Workspace join limit exceeded");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
