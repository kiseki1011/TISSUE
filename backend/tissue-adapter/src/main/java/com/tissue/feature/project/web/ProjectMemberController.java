package com.tissue.feature.project.web;

import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.usecase.ProjectMemberUseCase;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.project.web.request.AddProjectMembersRequest;
import com.tissue.feature.project.web.request.ChangeRoleRequest;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.WorkspaceErrors;
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

    @Operation(operationId = "addProjectMembers", summary = "Add members in batch", description = """
                Add multiple workspace members to the project at once. Up to 100 members can be added.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Members added"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @PostMapping("/batch")
    public ResponseEntity<ProjectMembersResponse> addProjectMembers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid AddProjectMembersRequest request,
            @CurrentMember MemberDetails memberDetails) {
        ProjectMembersResponse response = commandUseCase.addMembers(
                ProjectIdentifier.of(workspaceKey, projectKey), request.targetMemberIds(), memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "joinProject", summary = "Join project", description = """
                Join the project directly as a member.\
                 Only available for public projects or when the workspace role permits it.""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Joined project"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @ProjectErrors({
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_JOIN_NOT_ALLOWED,
    })
    @PostMapping(":join")
    public ResponseEntity<ProjectMemberResponse> joinProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        ProjectMemberResponse response =
                commandUseCase.join(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "updateProjectMemberRole", summary = "Change member role", description = """
                Change a project member's role.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role
                - Cannot modify members with equal or higher authority""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Role changed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED,
    })
    @PatchMapping("/{targetMemberId}/role")
    public ResponseEntity<Void> updateProjectMemberRole(
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

    @Operation(operationId = "kickProjectMember", summary = "Kick member", description = """
                Remove a member from the project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role
                - Cannot kick members with equal or higher authority""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Member kicked"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.SELF_KICK_NOT_ALLOWED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_MANAGER_MODIFICATION_NOT_ALLOWED,
    })
    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> kickProjectMember(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        commandUseCase.kickMember(
                ProjectIdentifier.of(workspaceKey, projectKey), targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "leaveProject", summary = "Leave project", description = "Leave the project voluntarily.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Left project"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @DeleteMapping
    public ResponseEntity<Void> leaveProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        commandUseCase.leave(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
