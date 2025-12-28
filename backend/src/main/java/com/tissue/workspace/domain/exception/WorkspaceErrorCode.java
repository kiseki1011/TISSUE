package com.tissue.workspace.domain.exception;

import com.tissue.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {
    WORKSPACE_NOT_FOUND("Workspace not found"),
    WORKSPACE_ARCHIVED("Workspace is archived"),
    WORKSPACE_MEMBER_NOT_FOUND("Workspace member not found"),
    WORKSPACE_MEMBER_LIMIT_EXCEEDED("Exceeded the maximum number of members in the workspace"),
    WORKSPACE_OWNERSHIP_REQUIRED("Workspace ownership is required for this operation"),

    INVITATION_NOT_FOUND("Invitation not found"),
    INVITATION_ALREADY_PROCESSED("Invitation is already processed"),

    INVITE_LINK_NOT_FOUND("Invite link not found"),
    INVALID_INVITE_LINK("Invite link is invalid or expired"),

    OWNER_CANNOT_LEAVE_WORKSPACE("Owner cannot leave the workspace"),
    CANNOT_CHANGE_ROLE_TO_OWNER("Cannot directly change workspace role to OWNER"),

    WORKSPACE_KEY_GENERATION_FAILED("Failed to generate unique workspace key"),
    INVALID_WORKSPACE_KEY_FORMAT("Invalid workspace key format");

    private final String defaultMessage;
}
