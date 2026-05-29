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
    LAST_SUPER_ADMIN(HttpStatus.CONFLICT, "Cannot demote or remove the last SUPER_ADMIN"),
    CANNOT_DEMOTE_SELF_SUPER_ADMIN(HttpStatus.CONFLICT, "A SUPER_ADMIN cannot demote themselves"),
    SYSTEM_ADMIN_REQUIRED(HttpStatus.FORBIDDEN, "System admin privilege required");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
