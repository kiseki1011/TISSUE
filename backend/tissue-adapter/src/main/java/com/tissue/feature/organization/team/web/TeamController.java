package com.tissue.feature.organization.team.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.organization.team.application.dto.response.TeamResponse;
import com.tissue.feature.organization.team.application.port.usecase.TeamUseCase;
import com.tissue.feature.organization.team.domain.exception.TeamErrorCode;
import com.tissue.feature.organization.team.web.request.CreateTeamRequest;
import com.tissue.feature.organization.team.web.request.UpdateTeamRequest;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.TeamErrors;
import com.tissue.security.adapter.web.annotation.RequireSystemAdmin;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Team")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TeamController {

    private final TeamUseCase teamUseCase;

    @Operation(operationId = "createTeam", summary = "Create team", description = """
                Create a new global team. (ex: "Infra", "DevOps")

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Team created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @TeamErrors({TeamErrorCode.DUPLICATE_TEAM_NAME})
    @RequireSystemAdmin
    @PostMapping("/teams")
    public ResponseEntity<TeamResponse> createTeam(
            @RequestBody @Valid CreateTeamRequest req, @CurrentMember MemberDetails memberDetails) {
        TeamResponse response = teamUseCase.create(req.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateTeam", summary = "Update team", description = """
                Update a team's name, description, or color. Only provided fields are updated.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @TeamErrors({
        TeamErrorCode.TEAM_NOT_FOUND,
        TeamErrorCode.DUPLICATE_TEAM_NAME,
    })
    @RequireSystemAdmin
    @PatchMapping("/teams/{teamId}")
    public ResponseEntity<Void> updateTeam(
            @PathVariable Long teamId,
            @RequestBody @Valid UpdateTeamRequest request,
            @CurrentMember MemberDetails memberDetails) {
        teamUseCase.update(teamId, request.toCommand(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteTeam", summary = "Delete team", description = """
                Permanently delete a global team. Any member currently belonging to it is unassigned.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Team deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @TeamErrors({TeamErrorCode.TEAM_NOT_FOUND})
    @RequireSystemAdmin
    @DeleteMapping("/teams/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId, @CurrentMember MemberDetails memberDetails) {
        teamUseCase.delete(teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
