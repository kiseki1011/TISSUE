package com.tissue.member.domain.exception;

import com.tissue.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	ACTIVE_MEMBER_NOT_FOUND("Active member not found"),
	MEMBER_NOT_FOUND("Member not found"),
	DUPLICATE_EMAIL("This email is already in use"),
	DUPLICATE_USERNAME("This username is already in use"),
	MEMBER_SIGNUP_CONFLICT("Member signup failed due to duplicate email or username"),
	OWNER_NOT_WITHDRAWABLE("Cannot withdraw if you're a workspace owner"),
	WORKSPACE_OWNAGE_LIMIT_EXCEEDED("Workspace ownage limit exceeded"),
	WORKSPACE_JOIN_LIMIT_EXCEEDED("Workspace join limit exceeded"),

	VERIFICATION_TOKEN_DUPLICATE("A verification email was already sent recently");

	private final String defaultMessage;
}
