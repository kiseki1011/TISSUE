package com.tissue.feature.workspace.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceCommandUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.feature.workspace.web.request.CreateWorkspaceRequest;
import com.tissue.feature.workspace.web.request.UpdateWorkspaceInfoRequest;
import com.tissue.global.openapi.MemberErrors;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceCommandController {

    private final WorkspaceCommandUseCase workspaceCommandUseCase;
    private final WorkspaceCreationGuard workspaceCreationGuard;

    @Operation(operationId = "createWorkspace", summary = "Create workspace", description = """
                Create a new workspace. The creator becomes the workspace owner.

                **Requirements:**
                - `workspaceKey` must be unique across the system
                - In single-tenant deployment mode, only system admins (`ROLE_ADMIN`) can create workspaces""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Workspace created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.INVALID_WORKSPACE_KEY_FORMAT,
        WorkspaceErrorCode.DUPLICATE_WORKSPACE_KEY,
        WorkspaceErrorCode.WORKSPACE_CREATE_ADMIN_ONLY,
    })
    @MemberErrors({
        MemberErrorCode.MEMBER_NOT_FOUND,
        MemberErrorCode.MEMBER_DELETED,
        MemberErrorCode.WORKSPACE_OWNAGE_LIMIT_EXCEEDED,
        MemberErrorCode.WORKSPACE_JOIN_LIMIT_EXCEEDED,
    })
    @PostMapping
    public ResponseEntity<WorkspaceCreateResponse> createWorkspace(
            @RequestBody @Valid CreateWorkspaceRequest request, @CurrentMember MemberDetails memberDetails) {
        workspaceCreationGuard.ensureAllowed(memberDetails);

        var command = request.toCommand();
        WorkspaceCreateResponse response = workspaceCommandUseCase.create(command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateWorkspace", summary = "Update workspace", description = """
                Update the workspace name or description. Only provided fields are updated.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
    })
    @PatchMapping("/{workspaceKey}")
    public ResponseEntity<Void> updateWorkspace(
            @PathVariable String workspaceKey,
            @RequestBody @Valid UpdateWorkspaceInfoRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workspaceCommandUseCase.update(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteWorkspace", summary = "Delete workspace", description = """
                Soft-delete the workspace. The workspace can be restored within the retention period.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace deleted"),
        @ApiResponse(responseCode = "403", description = "Only the owner can delete", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @DeleteMapping("/{workspaceKey}")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceCommandUseCase.delete(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "transferWorkspaceOwnership", summary = "Transfer ownership", description = """
                Transfer workspace ownership to another member.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Ownership transferred"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Only the owner can transfer ownership", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_ARCHIVED,
    })
    @PostMapping("/{workspaceKey}/members/{targetMemberId}:transferOwnership")
    public ResponseEntity<Void> transferWorkspaceOwnership(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceCommandUseCase.transferOwnership(workspaceKey, targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "archiveWorkspace", summary = "Archive workspace", description = """
                Archive the workspace. Archived workspaces are read-only and can be restored later.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace archived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @PostMapping("/{workspaceKey}:archive")
    public ResponseEntity<Void> archiveWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceCommandUseCase.archive(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "unarchiveWorkspace", summary = "Unarchive workspace", description = """
                Restore an archived workspace to a modifiable state.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace unarchived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
    })
    @PostMapping("/{workspaceKey}:unarchive")
    public ResponseEntity<Void> unarchiveWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceCommandUseCase.restoreArchived(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "restoreDeletedWorkspace", summary = "Restore deleted workspace", description = """
                Restore a soft-deleted workspace within the retention period.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace restored"),
        @ApiResponse(responseCode = "403", description = "Only the owner can restore", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
        WorkspaceErrorCode.INSUFFICIENT_WORKSPACE_ROLE,
        WorkspaceErrorCode.WORKSPACE_NOT_FOUND,
    })
    @PostMapping("/{workspaceKey}:restore")
    public ResponseEntity<Void> restoreDeletedWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceCommandUseCase.restoreDeleted(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
