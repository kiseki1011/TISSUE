package com.tissue.feature.workspace.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {
    DUPLICATE_WORKSPACE_KEY("Workspace key must be unique"),
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
    INVALID_WORKSPACE_KEY_FORMAT("Invalid workspace key format"),

    INSUFFICIENT_WORKSPACE_ROLE("Insufficient workspace role"),
    WORKSPACE_ADMIN_OR_SELF_REQUIRED("Workspace admin role or self-modification required"),
    ROLE_GRANT_NOT_ALLOWED("Insufficient permission to grant this role"),
    INVITE_LINK_EDIT_NOT_ALLOWED("Insufficient permission to edit invite link");

    private final String defaultMessage;
}
