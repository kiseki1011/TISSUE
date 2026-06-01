package com.tissue.feature.project.adapter.web;

import com.tissue.feature.project.application.dto.response.ProjectDetail;
import com.tissue.feature.project.application.dto.response.ProjectSummary;
import com.tissue.feature.project.application.port.usecase.ProjectQueryUseCase;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.shared.auth.CurrentMember;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Project")
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectQueryController {

    private final ProjectQueryUseCase projectQueryUseCase;

    @Operation(operationId = "listProjects", summary = "List projects", description = """
                    List all projects. Visible to any authenticated member \
                    regardless of project visibility. Joining `PRIVATE` projects requires an invite. \
                    Archived projects are excluded by default. Pass `includeArchived=true` to include them. \
                    Can search by keyword (matches title and key, case-insensitive).

                    **Requirements:**
                    - Requires authentication""")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Projects retrieved")})
    @GetMapping
    public ResponseEntity<Page<ProjectSummary>> listProjects(
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(required = false) @Nullable String keyword,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<ProjectSummary> response =
                projectQueryUseCase.getProjects(includeArchived, keyword, pageable, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "getProjectDetail", summary = "Get project detail", description = """
                Get project metadata.

                **Requirements:**
                - Requires authentication""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Project not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND})
    @GetMapping("/{projectKey}")
    public ResponseEntity<ProjectDetail> getProjectDetail(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        ProjectDetail response = projectQueryUseCase.getProjectDetail(
                ProjectIdentifier.ofProjectKey(projectKey), memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
