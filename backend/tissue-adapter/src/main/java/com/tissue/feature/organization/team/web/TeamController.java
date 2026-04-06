package com.tissue.feature.organization.team.web;

import com.tissue.feature.organization.team.application.dto.response.TeamCreateResponse;
import com.tissue.feature.organization.team.application.dto.response.TeamDetail;
import com.tissue.feature.organization.team.application.dto.response.TeamDetailList;
import com.tissue.feature.organization.team.application.port.usecase.TeamUseCase;
import com.tissue.feature.organization.team.web.request.CreateTeamRequest;
import com.tissue.feature.organization.team.web.request.UpdateTeamRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Team")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamUseCase teamUseCase;

    @Operation(summary = "Create team", description = """
                Create a new team within a workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Team created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Team name already exists", content = @Content)
    })
    @PostMapping
    public ResponseEntity<TeamCreateResponse> createTeam(
            @PathVariable String workspaceKey,
            @Valid @RequestBody CreateTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        TeamCreateResponse response = teamUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{teamId}")
                .buildAndExpand(response.teamId())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update team", description = """
                Update a team's name or description. Only provided fields are updated.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Team name already exists", content = @Content)
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

    @Operation(summary = "Delete team", description = """
                Delete a team from the workspace.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    })
    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(
            @PathVariable String workspaceKey, @PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {
        teamUseCase.delete(workspaceKey, teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get team detail", description = "Retrieve the detail of a team.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Team detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Team not found", content = @Content)
    })
    @GetMapping("/{teamId}")
    public ResponseEntity<TeamDetail> getTeamDetail(
            @PathVariable String workspaceKey, @PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {
        TeamDetail response = teamUseCase.getTeam(workspaceKey, teamId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List teams", description = "Retrieve all teams in the workspace.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Teams retrieved"),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @GetMapping
    public ResponseEntity<TeamDetailList> getWorkspaceTeams(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        TeamDetailList response = teamUseCase.getWorkspaceTeams(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
