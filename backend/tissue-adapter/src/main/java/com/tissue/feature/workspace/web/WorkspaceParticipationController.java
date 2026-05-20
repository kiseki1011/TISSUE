package com.tissue.feature.workspace.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceParticipationUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.feature.workspace.web.request.InviteToWorkspaceRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Participation")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class WorkspaceParticipationController {

    private final WorkspaceParticipationUseCase workspaceParticipationUseCase;

    @Operation(operationId = "inviteToWorkspace", summary = "Invite members", description = """
                Invite members to the workspace by email.\
                 Up to 50 emails can be invited at once.\
                 Optionally specify target projects for the invitees to auto-join.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invitations sent"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @PostMapping("/invitations")
    public ResponseEntity<InviteMembersResponse> inviteToWorkspace(
            @PathVariable String workspaceKey,
            @RequestBody @Valid InviteToWorkspaceRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        InviteMembersResponse response =
                workspaceParticipationUseCase.inviteToWorkspace(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "leaveWorkspace", summary = "Leave workspace", description = """
                Leave the workspace.\
                 The workspace owner cannot leave without transferring ownership first.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Left workspace"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.OWNER_CANNOT_LEAVE_WORKSPACE,
    })
    @DeleteMapping("/members/me")
    public ResponseEntity<Void> leaveWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceParticipationUseCase.leave(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "kickWorkspaceMember", summary = "Kick member", description = """
                Remove a member from the workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member kicked"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @DeleteMapping("/members/{targetMemberId}")
    public ResponseEntity<Void> kickWorkspaceMember(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceParticipationUseCase.kick(workspaceKey, targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
