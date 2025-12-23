package com.tissue.workspace.domain.exception;

import static com.tissue.common.exception.ContextKeys.*;
import static com.tissue.workspace.domain.exception.WorkspaceErrorCode.*;

import com.tissue.common.exception.base.BadRequestException;
import com.tissue.common.exception.base.ForbiddenException;
import com.tissue.common.exception.base.InternalServerException;
import com.tissue.common.exception.base.ResourceNotFoundException;
import com.tissue.workspace.domain.Invitation;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;

public class WorkspaceExceptions {

	private WorkspaceExceptions() {
	}

	public static ResourceNotFoundException notFound(String workspaceKey) {
		return new ResourceNotFoundException(WORKSPACE_NOT_FOUND)
			.addContext(WORKSPACE_KEY, workspaceKey);
	}

	public static BadRequestException archived(Workspace workspace) {
		return new BadRequestException(WORKSPACE_ARCHIVED)
			.addContext(WORKSPACE_KEY, workspace.getKey());
	}

	public static ResourceNotFoundException memberNotFound(Long memberId, String workspaceKey) {
		return new ResourceNotFoundException(WORKSPACE_MEMBER_NOT_FOUND)
			.addContext(MEMBER_ID, memberId)
			.addContext(WORKSPACE_KEY, workspaceKey);
	}

	public static BadRequestException memberLimitExceeded(String workspaceKey, int limit) {
		return new BadRequestException(WORKSPACE_MEMBER_LIMIT_EXCEEDED)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(LIMIT, limit);
	}

	public static ForbiddenException ownershipRequired(WorkspaceMember member) {
		return new ForbiddenException(WORKSPACE_OWNERSHIP_REQUIRED)
			.addContext(WORKSPACE_KEY, member.getWorkspaceKey())
			.addContext(MEMBER_ID, member.getMemberId())
			.addContext("currentRole", member.getRole());
	}

	public static ResourceNotFoundException invitationNotFound(Long invitationId) {
		return new ResourceNotFoundException(INVITATION_NOT_FOUND)
			.addContext(INVITATION_ID, invitationId);
	}

	public static BadRequestException invitationAlreadyProcessed(Invitation invitation) {
		return new BadRequestException(INVITATION_ALREADY_PROCESSED)
			.addContext(INVITATION_ID, invitation.getId())
			.addContext(STATUS, invitation.getStatus());
	}

	public static ResourceNotFoundException linkNotFound(String workspaceKey, String token) {
		return new ResourceNotFoundException(INVITE_LINK_NOT_FOUND)
			.addContext(WORKSPACE_KEY, workspaceKey)
			.addContext(TOKEN, token);
	}

	public static BadRequestException invalidLink(WorkspaceInviteLink link) {
		return new BadRequestException(INVALID_INVITE_LINK)
			.addContext(WORKSPACE_KEY, link.getWorkspaceKey())
			.addContext(TOKEN, link.getToken());
	}

	public static BadRequestException ownerCannotLeave(WorkspaceMember member) {
		return new BadRequestException(OWNER_CANNOT_LEAVE_WORKSPACE)
			.addContext(MEMBER_ID, member.getMemberId())
			.addContext(WORKSPACE_KEY, member.getWorkspaceKey());
	}

	public static BadRequestException cannotChangeRoleToOwner() {
		return new BadRequestException(CANNOT_CHANGE_ROLE_TO_OWNER);
	}

	public static InternalServerException keyGenerationFailed(Throwable e) {
		return new InternalServerException(WORKSPACE_KEY_GENERATION_FAILED, e);
	}
}
