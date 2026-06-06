package com.tissue.admin.adapter.web;

import com.tissue.admin.application.port.usecase.AdminProjectUseCase;
import com.tissue.feature.project.application.dto.response.ProjectHardDeletePreview;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.auth.RequireSuperAdmin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Lifecycle Management")
@RestController
@RequestMapping("/api/v1/admin/projects")
@RequiredArgsConstructor
@RequireSuperAdmin
public class AdminProjectController {

    private final AdminProjectUseCase adminProjectUseCase;

    @Operation(
            operationId = "adminPreviewProjectHardDelete",
            summary = "Preview a project hard-delete",
            description = """
                Count the resources that a permanent delete would remove. The project must already be soft-deleted.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview computed"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Project is not soft-deleted", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND, ProjectErrorCode.PROJECT_NOT_SOFT_DELETED})
    @GetMapping("/{projectKey}/hard/preview")
    public ResponseEntity<ProjectHardDeletePreview> previewHardDelete(@PathVariable String projectKey) {
        return ResponseEntity.ok(adminProjectUseCase.previewHardDelete(projectKey));
    }

    @Operation(
            operationId = "adminHardDeleteProject",
            summary = "Permanently delete a soft-deleted project",
            description = """
                Permanently delete a soft-deleted project and every resource that hangs off it including the
                issue subtree, sprints, tags, project members, VCS integrations, activity logs and stored attachment files.

                The project must already be soft-deleted, and the `confirm` query parameter must exactly equal the
                project key. Returns the counts of what was removed.

                **Requirements:**
                - Requires system `SUPER_ADMIN` role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project permanently deleted"),
        @ApiResponse(responseCode = "400", description = "Confirmation key mismatch", content = @Content),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Project is not soft-deleted", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_SOFT_DELETED,
        ProjectErrorCode.HARD_DELETE_CONFIRMATION_MISMATCH
    })
    @DeleteMapping("/{projectKey}/hard")
    public ResponseEntity<ProjectHardDeletePreview> hardDelete(
            @PathVariable String projectKey,
            @RequestParam("confirm") String confirm,
            @CurrentMember MemberDetails memberDetails) {
        ProjectHardDeletePreview result =
                adminProjectUseCase.hardDelete(projectKey, confirm, memberDetails.getMemberId());
        return ResponseEntity.ok(result);
    }
}
