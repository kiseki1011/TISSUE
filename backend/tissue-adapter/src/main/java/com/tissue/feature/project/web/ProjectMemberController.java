package com.tissue.feature.project.web;

import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.usecase.ProjectMemberUseCase;
import com.tissue.feature.project.web.request.AddProjectMembersRequest;
import com.tissue.feature.project.web.request.ChangeRoleRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
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

@Tag(name = "Project Member")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/members")
public class ProjectMemberController {

    private final ProjectMemberUseCase commandUseCase;

    @Operation(summary = "Add members in batch", description = """
                Add multiple workspace members to the project at once. Up to 100 members can be added.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Members added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content)
    })
    @PostMapping("/batch")
    public ResponseEntity<ProjectMembersResponse> addMembers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid AddProjectMembersRequest request,
            @CurrentMember MemberDetails memberDetails) {
        ProjectMembersResponse response = commandUseCase.addMembers(
                ProjectIdentifier.of(workspaceKey, projectKey), request.targetMemberIds(), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Join project", description = """
                Join the project directly as a member.\
                 Only available for public projects or when the workspace role permits it.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined project"),
        @ApiResponse(responseCode = "403", description = "Cannot join this project", content = @Content),
        @ApiResponse(responseCode = "409", description = "Already a project member", content = @Content)
    })
    @PostMapping(":join")
    public ResponseEntity<ProjectMemberResponse> joinProjectDirectly(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        ProjectMemberResponse response =
                commandUseCase.join(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Change member role", description = """
                Change a project member's role.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role
                - Cannot modify members with equal or higher authority""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role changed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Target member not found", content = @Content)
    })
    @PatchMapping("/{targetMemberId}/role")
    public ResponseEntity<Void> changeRole(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long targetMemberId,
            @RequestBody @Valid ChangeRoleRequest request,
            @CurrentMember MemberDetails memberDetails) {
        commandUseCase.changeRole(
                ProjectIdentifier.of(workspaceKey, projectKey),
                targetMemberId,
                request.role(),
                memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Kick member", description = """
                Remove a member from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role
                - Cannot kick members with equal or higher authority""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member kicked"),
        @ApiResponse(responseCode = "400", description = "Cannot kick yourself", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Target member not found", content = @Content)
    })
    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        commandUseCase.kickMember(
                ProjectIdentifier.of(workspaceKey, projectKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Leave project", description = "Leave the project voluntarily.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Left project"),
        @ApiResponse(responseCode = "404", description = "Not a member of this project", content = @Content)
    })
    @DeleteMapping
    public ResponseEntity<Void> leaveProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        commandUseCase.leave(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
