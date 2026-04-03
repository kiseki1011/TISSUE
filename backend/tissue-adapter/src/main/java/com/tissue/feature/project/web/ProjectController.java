package com.tissue.feature.project.web;

import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.usecase.ProjectUseCase;
import com.tissue.feature.project.web.request.CreateProjectRequest;
import com.tissue.feature.project.web.request.UpdateProjectRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Tag(name = "Project")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects")
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    @Operation(summary = "Create project", description = """
                Create a new project within the workspace. The creator becomes the project manager.

                **Requirements:**
                - `projectKey` must be unique within the workspace""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Project created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(
                responseCode = "409",
                description = "Project key already exists in this workspace",
                content = @Content)
    })
    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateProjectRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        ProjectResponse response = projectUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{projectKey}")
                .buildAndExpand(response.projectKey())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @Operation(summary = "Update project", description = """
                Update the project title, description, or visibility. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @PatchMapping("/{projectKey}")
    public ResponseEntity<Void> update(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid UpdateProjectRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        projectUseCase.update(ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete project", description = """
                Soft-delete the project. The project can be restored within the retention period.

                **Requirements:**
                - Requires workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @DeleteMapping("/{projectKey}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        projectUseCase.delete(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Archive project", description = """
                Archive the project. Archived projects are read-only and can be restored later.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project archived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @PostMapping("/{projectKey}:archive")
    public ResponseEntity<Void> archive(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        projectUseCase.archive(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Unarchive project", description = """
                Restore an archived project to a modifiable state.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project unarchived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @PostMapping("/{projectKey}:unarchive")
    public ResponseEntity<Void> restoreArchived(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        projectUseCase.restoreArchived(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Restore deleted project", description = """
                Restore a soft-deleted project within the retention period.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project restored"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(
                responseCode = "404",
                description = "Project not found or retention period expired",
                content = @Content)
    })
    @PostMapping("/{projectKey}:restore")
    public ResponseEntity<Void> restoreDeleted(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {
        projectUseCase.restoreDeleted(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
