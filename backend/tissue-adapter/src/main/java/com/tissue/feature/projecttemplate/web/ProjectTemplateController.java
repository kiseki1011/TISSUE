package com.tissue.feature.projecttemplate.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateResponse;
import com.tissue.feature.projecttemplate.application.port.usecase.ProjectTemplateUseCase;
import com.tissue.feature.projecttemplate.domain.exception.ProjectTemplateErrorCode;
import com.tissue.feature.projecttemplate.web.request.CreateTemplateFromProjectRequest;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.ProjectTemplateErrors;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Template")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class ProjectTemplateController {

    private final ProjectTemplateUseCase projectTemplateUseCase;

    @Operation(
            operationId = "createTemplateFromProject",
            summary = "Create project template from project",
            description = """
                Create a new project template from an existing project's configuration.

                The configuration includes:
                - Workflows (with states, transitions, and guards)
                - Issue types (with fields and options)

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Template created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @PostMapping("/templates:fromProject")
    public ResponseEntity<ProjectTemplateResponse> createTemplateFromProject(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateTemplateFromProjectRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand(workspaceKey);
        ProjectTemplateResponse response =
                projectTemplateUseCase.createFromProject(command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "deleteProjectTemplate", summary = "Delete project template", description = """
                Permanently delete a project template from the workspace.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Template deleted"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectTemplateErrors({ProjectTemplateErrorCode.PROJECT_TEMPLATE_NOT_FOUND})
    @DeleteMapping("/templates/{templateId}")
    public ResponseEntity<Void> deleteProjectTemplate(
            @PathVariable String workspaceKey,
            @PathVariable Long templateId,
            @CurrentMember MemberDetails memberDetails) {
        projectTemplateUseCase.delete(workspaceKey, templateId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
