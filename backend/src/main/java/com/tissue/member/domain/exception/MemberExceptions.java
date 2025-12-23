package com.tissue.member.domain.exception;

import static com.tissue.global.exception.ContextKeys.*;
import static com.tissue.member.domain.exception.MemberErrorCode.*;
import static com.tissue.security.authentication.exception.AuthenticationErrorCode.*;

import com.tissue.global.exception.base.BadRequestException;
import com.tissue.global.exception.base.ForbiddenException;
import com.tissue.global.exception.base.ResourceConflictException;
import com.tissue.global.exception.base.ResourceNotFoundException;
import com.tissue.member.domain.Member;

public final class MemberExceptions {

	private MemberExceptions() {
	}

	public static ResourceNotFoundException activeNotFound(Long memberId) {
		return new ResourceNotFoundException(ACTIVE_MEMBER_NOT_FOUND)
			.addContext(MEMBER_ID, memberId);
	}

	public static ResourceNotFoundException notFound(Long memberId) {
		return new ResourceNotFoundException(MEMBER_NOT_FOUND)
			.addContext(MEMBER_ID, memberId);
	}

	public static ResourceConflictException duplicateEmail(String email) {
		return new ResourceConflictException(DUPLICATE_EMAIL)
			.addContext(EMAIL, email);
	}

	public static ResourceConflictException duplicateEmail(String email, Throwable e) {
		return new ResourceConflictException(DUPLICATE_EMAIL, e)
			.addContext(EMAIL, email);
	}

	public static ResourceConflictException duplicateUsername(String username) {
		return new ResourceConflictException(DUPLICATE_USERNAME)
			.addContext(USERNAME, username);
	}

	public static ResourceConflictException duplicateUsername(String username, Throwable e) {
		return new ResourceConflictException(DUPLICATE_USERNAME, e)
			.addContext(USERNAME, username);
	}

	public static ResourceConflictException signUpConflict(String email, String username, Throwable e) {
		return new ResourceConflictException(MEMBER_SIGNUP_CONFLICT, e)
			.addContext(EMAIL, email)
			.addContext(USERNAME, username);
	}

	public static BadRequestException ownerNotWithdrawable(Member member) {
		return new BadRequestException(OWNER_NOT_WITHDRAWABLE)
			.addContext(MEMBER_ID, member.getId())
			.addContext(USERNAME, member.getUsername());
	}

	public static BadRequestException workspaceOwnageLimit(Member member, int limit) {
		return new BadRequestException(WORKSPACE_OWNAGE_LIMIT_EXCEEDED)
			.addContext(MEMBER_ID, member.getId())
			.addContext(USERNAME, member.getUsername())
			.addContext("workspaceCreateLimit", limit);
	}

	public static BadRequestException workspaceJoinLimit(Member member, int limit) {
		return new BadRequestException(WORKSPACE_JOIN_LIMIT_EXCEEDED)
			.addContext(MEMBER_ID, member.getId())
			.addContext(USERNAME, member.getUsername())
			.addContext("workspaceJoinLimit", limit);
	}

	public static ResourceConflictException verificationTokenDuplicate(String email) {
		return new ResourceConflictException(VERIFICATION_TOKEN_DUPLICATE)
			.addContext(EMAIL, email);
	}

	public static ResourceConflictException verificationTokenDuplicate(String email, Throwable e) {
		return new ResourceConflictException(VERIFICATION_TOKEN_DUPLICATE, e)
			.addContext(EMAIL, email);
	}

	public static ForbiddenException emailNotVerified(String email) {
		return new ForbiddenException(EMAIL_NOT_VERIFIED)
			.addContext(EMAIL, email);
	}
}
