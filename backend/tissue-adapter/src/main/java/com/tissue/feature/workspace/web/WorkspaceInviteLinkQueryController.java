package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkQueryUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Invite Link")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceInviteLinkQueryController {

    private final WorkspaceLinkQueryUseCase workspaceLinkQueryUseCase;

    @Operation(operationId = "listWorkspaceInviteLinks", summary = "List invite links", description = """
                    List all active invite links for the workspace.

                    **Requirements:**
                    - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invite link list retrieved"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @GetMapping("/inviteLinks")
    public ResponseEntity<List<WorkspaceInviteLinkDetail>> listWorkspaceInviteLinks(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        List<WorkspaceInviteLinkDetail> response =
                workspaceLinkQueryUseCase.getWorkspaceLinks(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getWorkspaceInviteLink", summary = "Get invite link detail", description = """
                    Get detailed information about a specific invite link.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Invite link detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INVITE_LINK_NOT_FOUND,
    })
    @GetMapping("/inviteLinks/{token}")
    public ResponseEntity<WorkspaceInviteLinkDetail> getWorkspaceInviteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {
        WorkspaceInviteLinkDetail response =
                workspaceLinkQueryUseCase.getLinkDetail(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
