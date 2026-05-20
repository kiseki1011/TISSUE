package com.tissue.feature.organization.team.web;

import com.tissue.feature.organization.team.application.dto.response.TeamCreateResponse;
import com.tissue.feature.organization.team.application.dto.response.TeamDetail;
import com.tissue.feature.organization.team.application.dto.response.TeamDetailList;
import com.tissue.feature.organization.team.application.port.usecase.TeamUseCase;
import com.tissue.feature.organization.team.domain.exception.TeamErrorCode;
import com.tissue.feature.organization.team.web.request.CreateTeamRequest;
import com.tissue.feature.organization.team.web.request.UpdateTeamRequest;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.TeamErrors;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Team")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamUseCase teamUseCase;

    @Operation(operationId = "createTeam", summary = "Create team", description = """
                Create a new team within a workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Team created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.WORKSPACE_NOT_FOUND,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @TeamErrors({TeamErrorCode.DUPLICATE_TEAM_NAME})
    @PostMapping
    public ResponseEntity<TeamCreateResponse> createTeam(
            @PathVariable String workspaceKey,
            @Valid @RequestBody CreateTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        TeamCreateResponse response = teamUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateTeam", summary = "Update team", description = """
                Update a team's name or description. Only provided fields are updated.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
    })
    @TeamErrors({
        TeamErrorCode.TEAM_NOT_FOUND,
        TeamErrorCode.DUPLICATE_TEAM_NAME,
    })
    @PatchMapping("/{teamId}")
    public ResponseEntity<Void> updateTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long teamId,
            @Valid @RequestBody UpdateTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        teamUseCase.update(workspaceKey, teamId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteTeam", summary = "Delete team", description = """
                Permanently delete a team from the workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
    })
    @TeamErrors({
        TeamErrorCode.TEAM_NOT_FOUND,
        TeamErrorCode.TEAM_IN_USE,
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable String workspaceKey, @PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {
        teamUseCase.delete(workspaceKey, teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "getTeam", summary = "Get team detail", description = "Retrieve the detail of a team.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @TeamErrors({TeamErrorCode.TEAM_NOT_FOUND})
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetail> getTeam(
            @PathVariable String workspaceKey, @PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {
        TeamDetail response = teamUseCase.getTeam(workspaceKey, teamId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listTeams", summary = "List teams", description = "Retrieve all teams in the workspace.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Teams retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping
    public ResponseEntity<TeamDetailList> listTeams(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        TeamDetailList response = teamUseCase.getWorkspaceTeams(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
