package com.tissue.feature.workspace.web;

import com.tissue.feature.organization.position.domain.exception.PositionErrorCode;
import com.tissue.feature.organization.team.domain.exception.TeamErrorCode;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberManageUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.feature.workspace.web.request.UpdateRoleRequest;
import com.tissue.global.openapi.PositionErrors;
import com.tissue.global.openapi.TeamErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Member")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberManageUseCase workspaceMemberManageUseCase;

    @Operation(operationId = "updateWorkspaceMemberRole", summary = "Update member role", description = """
                Change a workspace member's role.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role
                - Cannot grant `OWNER` role
                - Can only change roles lower than the actor's own role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.CANNOT_CHANGE_ROLE_TO_OWNER,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.ROLE_GRANT_NOT_ALLOWED,
    })
    @PatchMapping("/{targetMemberId}/role")
    public ResponseEntity<Void> updateWorkspaceMemberRole(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @CurrentMember MemberDetails memberDetails) {
        workspaceMemberManageUseCase.updateRole(
                workspaceKey, targetMemberId, request.role(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addPositionToWorkspaceMember", summary = "Add position to member", description = """
                Assign a position to a workspace member.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position added"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @PositionErrors({PositionErrorCode.POSITION_NOT_FOUND})
    @PutMapping("/{targetMemberId}/positions/{positionId}")
    public ResponseEntity<Void> addPositionToWorkspaceMember(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceMemberManageUseCase.addPosition(workspaceKey, targetMemberId, positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "removePositionFromWorkspaceMember",
            summary = "Remove position from member",
            description = """
                Remove a position assignment from a workspace member.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Position removed"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @PositionErrors({PositionErrorCode.POSITION_NOT_FOUND})
    @DeleteMapping("/{targetMemberId}/positions/{positionId}")
    public ResponseEntity<Void> removePositionFromWorkspaceMember(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceMemberManageUseCase.removePosition(
                workspaceKey, targetMemberId, positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addTeamToWorkspaceMember", summary = "Add team to member", description = """
                Assign a team to a workspace member.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team added"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @TeamErrors({TeamErrorCode.TEAM_NOT_FOUND})
    @PutMapping("/{targetMemberId}/teams/{teamId}")
    public ResponseEntity<Void> addTeamToWorkspaceMember(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long teamId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceMemberManageUseCase.addTeam(workspaceKey, targetMemberId, teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "removeTeamFromWorkspaceMember", summary = "Remove team from member", description = """
                Remove a team assignment from a workspace member.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team removed"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @TeamErrors({TeamErrorCode.TEAM_NOT_FOUND})
    @DeleteMapping("/{targetMemberId}/teams/{teamId}")
    public ResponseEntity<Void> removeTeamFromWorkspaceMember(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long teamId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceMemberManageUseCase.removeTeam(workspaceKey, targetMemberId, teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "searchWorkspaceMembers", summary = "Search members", description = """
                Search workspace members by name or username.\
                 Optionally filter by project membership using the `projectKey` parameter.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
    })
    @GetMapping("/search")
    public ResponseEntity<List<WorkspaceMemberSearchResponse>> searchWorkspaceMembers(
            @PathVariable String workspaceKey,
            @Parameter(description = "Search keyword for name or username") @RequestParam String query,
            @Parameter(description = "Filter by project membership") @RequestParam(required = false) @Nullable
                    String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        return ResponseEntity.ok(workspaceMemberManageUseCase.searchMembers(
                workspaceKey, projectKey, query, memberDetails.getMemberId()));
    }
}
