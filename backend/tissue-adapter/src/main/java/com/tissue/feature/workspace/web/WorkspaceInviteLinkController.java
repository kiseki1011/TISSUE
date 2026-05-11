package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.command.InviteLinkResponse;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkUseCase;
import com.tissue.feature.workspace.web.request.CreateWorkspaceInviteLinkRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Invite Link")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceInviteLinkController {

    private final WorkspaceLinkUseCase linkUseCase;

    @Operation(operationId = "createWorkspaceInviteLink", summary = "Create invite link", description = """
                Create a reusable invite link for the workspace.\
                 The link can optionally specify an expiration time and target projects to auto-join.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Invite link created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Target project not found", content = @Content)
    })
    @PostMapping("/inviteLinks")
    public ResponseEntity<InviteLinkResponse> createWorkspaceInviteLink(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateWorkspaceInviteLinkRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        String token = linkUseCase.createWorkspaceLink(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(new InviteLinkResponse(token, request.expiredAt()));
    }

    @Operation(operationId = "deleteWorkspaceInviteLink", summary = "Delete invite link", description = """
                Permanently deletes an existing invite link so it can no longer be used.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role, or be the link creator""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Invite link deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Invite link not found", content = @Content)
    })
    @DeleteMapping("/inviteLinks/{token}")
    public ResponseEntity<Void> deleteWorkspaceInviteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {
        linkUseCase.deleteLink(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "joinWorkspaceViaInviteLink", summary = "Join via invite link", description = """
                Join the workspace using an invite link token.\
                 The member will be assigned the role specified in the link.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined workspace successfully"),
        @ApiResponse(responseCode = "400", description = "Invite link is invalid or expired", content = @Content),
        @ApiResponse(responseCode = "404", description = "Invite link not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Already a member of this workspace", content = @Content)
    })
    @PostMapping("/inviteLinks/{token}:join")
    public ResponseEntity<WorkspaceMemberResponse> joinWorkspaceViaInviteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {
        WorkspaceMemberResponse response = linkUseCase.joinViaLink(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listWorkspaceInviteLinks", summary = "List invite links", description = """
                Retrieve all active invite links for the workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invite link list retrieved"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content)
    })
    @GetMapping("/inviteLinks")
    public ResponseEntity<List<WorkspaceInviteLinkDetail>> listWorkspaceInviteLinks(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        List<WorkspaceInviteLinkDetail> response =
                linkUseCase.getWorkspaceLinks(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "getWorkspaceInviteLink",
            summary = "Get invite link detail",
            description = "Retrieve detailed information about a specific invite link.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invite link detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Invite link not found", content = @Content)
    })
    @GetMapping("/inviteLinks/{token}")
    public ResponseEntity<WorkspaceInviteLinkDetail> getWorkspaceInviteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {
        WorkspaceInviteLinkDetail response =
                linkUseCase.getLinkDetail(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
