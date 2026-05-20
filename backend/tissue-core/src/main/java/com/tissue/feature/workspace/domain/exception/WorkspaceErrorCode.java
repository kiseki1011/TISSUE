package com.tissue.feature.workspace.domain.exception;

import com.tissue.shared.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WorkspaceErrorCode implements ErrorCode {
    DUPLICATE_WORKSPACE_KEY(HttpStatus.CONFLICT, "Workspace key must be unique"),
    WORKSPACE_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace not found"),
    WORKSPACE_ARCHIVED(HttpStatus.BAD_REQUEST, "Workspace is archived"),
    WORKSPACE_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "Workspace member not found"),
    WORKSPACE_MEMBER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Exceeded the maximum number of members in the workspace"),
    WORKSPACE_PROJECT_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "Exceeded the maximum number of projects in the workspace"),
    WORKSPACE_OWNERSHIP_REQUIRED(HttpStatus.FORBIDDEN, "Workspace ownership is required for this operation"),

    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "Invitation not found"),

    INVITE_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "Invite link not found"),
    INVALID_INVITE_LINK(HttpStatus.BAD_REQUEST, "Invite link is invalid or expired"),

    OWNER_CANNOT_LEAVE_WORKSPACE(HttpStatus.BAD_REQUEST, "Owner cannot leave the workspace"),
    CANNOT_CHANGE_ROLE_TO_OWNER(HttpStatus.BAD_REQUEST, "Cannot directly change workspace role to OWNER"),

    WORKSPACE_KEY_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to generate unique workspace key"),
    INVALID_WORKSPACE_KEY_FORMAT(HttpStatus.BAD_REQUEST, "Invalid workspace key format"),
    INVALID_DISPLAY_NAME_FORMAT(
            HttpStatus.BAD_REQUEST, "Invalid display name format. Must be 3-35 characters without special characters."),

    INSUFFICIENT_WORKSPACE_ROLE(HttpStatus.FORBIDDEN, "Insufficient workspace role"),
    WORKSPACE_ADMIN_OR_SELF_REQUIRED(HttpStatus.FORBIDDEN, "Workspace admin role or self-modification required"),
    ROLE_GRANT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Insufficient permission to grant this role"),
    INVITE_LINK_EDIT_NOT_ALLOWED(HttpStatus.FORBIDDEN, "Insufficient permission to edit invite link");

    private final HttpStatus httpStatus;
    private final String defaultMessage;
}
