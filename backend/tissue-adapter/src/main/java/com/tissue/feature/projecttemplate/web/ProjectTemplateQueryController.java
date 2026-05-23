package com.tissue.feature.projecttemplate.web;

import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateDetail;
import com.tissue.feature.projecttemplate.application.dto.response.ProjectTemplateSummary;
import com.tissue.feature.projecttemplate.application.port.usecase.ProjectTemplateQueryUseCase;
import com.tissue.feature.projecttemplate.domain.exception.ProjectTemplateErrorCode;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.ProjectTemplateErrors;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project Template")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class ProjectTemplateQueryController {

    private final ProjectTemplateQueryUseCase projectTemplateQueryUseCase;

    @Operation(operationId = "listWorkspaceTemplates", summary = "List workspace project templates", description = """
                    List project templates of a workspace. Each item contains only basic info. \
                    Use `getProjectTemplate` to fetch the full config.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Templates retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @GetMapping("/templates")
    public ResponseEntity<Page<ProjectTemplateSummary>> listWorkspaceTemplates(
            @PathVariable String workspaceKey, Pageable pageable, @CurrentMember MemberDetails memberDetails) {
        Page<ProjectTemplateSummary> response =
                projectTemplateQueryUseCase.getWorkspaceTemplates(workspaceKey, pageable, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectTemplate", summary = "Get project template detail", description = """
                    Get a single project template with its full configuration (workflows, issue types, fields). \
                    Use this when previewing a template before applying it to a new project.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Template detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND})
    @ProjectTemplateErrors({ProjectTemplateErrorCode.PROJECT_TEMPLATE_NOT_FOUND})
    @GetMapping("/templates/{templateId}")
    public ResponseEntity<ProjectTemplateDetail> getProjectTemplate(
            @PathVariable String workspaceKey,
            @PathVariable Long templateId,
            @CurrentMember MemberDetails memberDetails) {
        ProjectTemplateDetail response = projectTemplateQueryUseCase.getProjectTemplateDetail(
                workspaceKey, templateId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
