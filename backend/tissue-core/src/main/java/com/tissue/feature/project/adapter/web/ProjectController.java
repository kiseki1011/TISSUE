package com.tissue.feature.project.adapter.web;

import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.project.adapter.web.request.CreateProjectRequest;
import com.tissue.feature.project.adapter.web.request.UpdateProjectRequest;
import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.usecase.ProjectUseCase;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.MemberErrors;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
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

@Tag(name = "Project")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectUseCase projectUseCase;

    @Operation(operationId = "createProject", summary = "Create project", description = """
                Create a new project. The creator becomes the project manager.

                **Requirements:**
                - `projectKey` must be globally unique""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Project created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.INVALID_PROJECT_KEY_FORMAT,
        ProjectErrorCode.RESERVED_PROJECT_KEY,
        ProjectErrorCode.DUPLICATE_PROJECT_KEY,
    })
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(
            @RequestBody @Valid CreateProjectRequest request, @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        ProjectResponse response = projectUseCase.create(command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateProject", summary = "Update project", description = """
                Update the project title, description, or visibility. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @PatchMapping("/{projectKey}")
    public ResponseEntity<Void> updateProject(
            @PathVariable String projectKey,
            @RequestBody @Valid UpdateProjectRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        projectUseCase.update(ProjectIdentifier.ofProjectKey(projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteProject", summary = "Delete project", description = """
                Soft-delete the project. Can be restored later.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project deleted"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @DeleteMapping("/{projectKey}")
    public ResponseEntity<Void> deleteProject(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        projectUseCase.delete(ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "archiveProject", summary = "Archive project", description = """
                Archive the project. Archived projects are read-only and can be restored later.

                **Requirements:**
                - Requires project `MANAGER` or system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project archived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @PostMapping("/{projectKey}:archive")
    public ResponseEntity<Void> archiveProject(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        projectUseCase.archive(ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "unarchiveProject", summary = "Unarchive project", description = """
                Restore an archived project to a modifiable state.

                **Requirements:**
                - Requires project `MANAGER` or system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project unarchived"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @PostMapping("/{projectKey}:unarchive")
    public ResponseEntity<Void> unarchiveProject(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        projectUseCase.restoreArchived(ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "restoreDeletedProject", summary = "Restore deleted project", description = """
                Restore a soft-deleted project within the retention period.

                **Requirements:**
                - Requires system `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Project restored"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
    })
    @MemberErrors({MemberErrorCode.SYSTEM_ADMIN_REQUIRED})
    @PostMapping("/{projectKey}:restore")
    public ResponseEntity<Void> restoreDeletedProject(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        projectUseCase.restoreDeleted(ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
