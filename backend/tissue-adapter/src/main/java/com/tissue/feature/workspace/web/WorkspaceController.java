package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceUseCase;
import com.tissue.feature.workspace.web.request.CreateWorkspaceRequest;
import com.tissue.feature.workspace.web.request.UpdateWorkspaceInfoRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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

@Tag(name = "Workspace")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceUseCase workspaceUseCase;

    @Operation(summary = "Create workspace", description = """
                Create a new workspace. The creator becomes the workspace owner.

                **Requirements:**
                - `workspaceKey` must be unique across the system""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Workspace created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "409", description = "Workspace key already exists", content = @Content)
    })
    @PostMapping
    public ResponseEntity<WorkspaceCreateResponse> createWorkspace(
            @RequestBody @Valid CreateWorkspaceRequest request, @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        WorkspaceCreateResponse response = workspaceUseCase.create(command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{workspaceKey}")
                .buildAndExpand(response.workspaceKey())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update workspace", description = """
                Update the workspace name or description. Only provided fields are updated.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace updated"),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or workspace is archived",
                content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @PatchMapping("/{workspaceKey}")
    public ResponseEntity<Void> updateWorkspaceInfo(
            @PathVariable String workspaceKey,
            @RequestBody @Valid UpdateWorkspaceInfoRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workspaceUseCase.update(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete workspace", description = """
                Soft-delete the workspace. The workspace can be restored within the retention period.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace deleted"),
        @ApiResponse(responseCode = "403", description = "Only the owner can delete", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @DeleteMapping("/{workspaceKey}")
    public ResponseEntity<Void> delete(@PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.delete(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Transfer ownership", description = """
                Transfer workspace ownership to another member.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Ownership transferred"),
        @ApiResponse(responseCode = "403", description = "Only the owner can transfer ownership", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace or target member not found", content = @Content)
    })
    @PatchMapping("/{workspaceKey}/members/{targetMemberId}/ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.transferOwnership(workspaceKey, targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Get workspace detail",
            description = "Retrieve detailed information about a workspace including its settings.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Workspace detail retrieved"),
        @ApiResponse(responseCode = "403", description = "Not a member of this workspace", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @GetMapping("/{workspaceKey}")
    public ResponseEntity<WorkspaceDetail> getWorkspaceDetail(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        WorkspaceDetail response = workspaceUseCase.getDetail(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "List my workspaces", description = "Retrieve all workspaces the current member belongs to.")
    @ApiResponse(responseCode = "200", description = "Workspace list retrieved")
    @GetMapping("/me")
    public ResponseEntity<List<WorkspaceSummaryResponse>> listMyWorkspaces(@CurrentMember MemberDetails userDetails) {
        List<WorkspaceSummaryResponse> response = workspaceUseCase.getMyWorkspaces(userDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Archive workspace", description = """
                Archive the workspace. Archived workspaces are read-only and can be restored later.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace archived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @PostMapping("/{workspaceKey}/archive")
    public ResponseEntity<Void> archive(@PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.archive(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unarchive workspace", description = """
                Restore an archived workspace to a modifiable state.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace unarchived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Workspace not found", content = @Content)
    })
    @PostMapping("/{workspaceKey}/unarchive")
    public ResponseEntity<Void> restoreArchived(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.restoreArchived(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore deleted workspace", description = """
                Restore a soft-deleted workspace within the retention period.

                **Requirements:**
                - Requires workspace `OWNER` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Workspace restored"),
        @ApiResponse(responseCode = "403", description = "Only the owner can restore", content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "Workspace not found or retention period expired",
                content = @Content)
    })
    @PostMapping("/{workspaceKey}/restore")
    public ResponseEntity<Void> restoreDeleted(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.restoreDeleted(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "List deleted workspaces",
            description = "Retrieve all soft-deleted workspaces owned by the current member"
                    + " that are still within the retention period.")
    @ApiResponse(responseCode = "200", description = "Deleted workspace list retrieved")
    @GetMapping("/deleted")
    public ResponseEntity<List<DeletedWorkspaceSummary>> listMyDeletedWorkspaces(
            @CurrentMember MemberDetails memberDetails) {
        List<DeletedWorkspaceSummary> response = workspaceUseCase.getMyDeletedWorkspaces(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
