package com.tissue.member.domain.exception;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.member.domain.exception.MemberErrorCode.*;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.common.exception.base.ResourceConflictException;
import com.tissue.common.exception.base.ResourceNotFoundException;
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
			.addContext(MEMBER_ID, member.getUsername());
	}

	public static BadRequestException workspaceOwnageLimit(Member member, int limit) {
		return new BadRequestException(WORKSPACE_OWNAGE_LIMIT_EXCEEDED)
			.addContext(MEMBER_ID, member.getId())
			.addContext(MEMBER_ID, member.getUsername())
			.addContext("workspaceCreateLimit", limit);
	}

	public static BadRequestException workspaceJoinLimit(Member member, int limit) {
		return new BadRequestException(WORKSPACE_JOIN_LIMIT_EXCEEDED)
			.addContext(MEMBER_ID, member.getId())
			.addContext(MEMBER_ID, member.getUsername())
			.addContext("workspaceJoinLimit", limit);
	}
}
