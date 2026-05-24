package com.tissue.feature.workspace.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workspace.application.dto.response.command.InviteLinkResponse;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkCommandUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.feature.workspace.web.request.CreateWorkspaceInviteLinkRequest;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Invite Link")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceInviteLinkCommandController {

    private final WorkspaceLinkCommandUseCase workspaceLinkCommandUseCase;

    @Operation(operationId = "createWorkspaceInviteLink", summary = "Create invite link", description = """
                Create a reusable invite link for the workspace.\
                 The link can optionally specify an expiration time and target projects to auto-join.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invite link created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @PostMapping("/inviteLinks")
    public ResponseEntity<InviteLinkResponse> createWorkspaceInviteLink(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateWorkspaceInviteLinkRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        String token =
                workspaceLinkCommandUseCase.createWorkspaceLink(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new InviteLinkResponse(token, request.expiredAt()));
    }

    @Operation(operationId = "deleteWorkspaceInviteLink", summary = "Delete invite link", description = """
                Permanently deletes an existing invite link so it can no longer be used.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or be the link creator""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invite link deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.INVITE_LINK_NOT_FOUND,
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INVITE_LINK_EDIT_NOT_ALLOWED,
    })
    @DeleteMapping("/inviteLinks/{token}")
    public ResponseEntity<Void> deleteWorkspaceInviteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {
        workspaceLinkCommandUseCase.deleteLink(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "joinWorkspaceViaInviteLink", summary = "Join via invite link", description = """
                Join the workspace using an invite link token.\
                 The member will be assigned the role specified in the link.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined workspace successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.INVITE_LINK_NOT_FOUND,
        WorkspaceErrorCode.INVALID_INVITE_LINK,
        WorkspaceErrorCode.WORKSPACE_MEMBER_LIMIT_EXCEEDED,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
        MemberErrorCode.WORKSPACE_JOIN_LIMIT_EXCEEDED,
    })
    @PostMapping("/inviteLinks/{token}:join")
    public ResponseEntity<WorkspaceMemberResponse> joinWorkspaceViaInviteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {
        WorkspaceMemberResponse response =
                workspaceLinkCommandUseCase.joinViaLink(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
