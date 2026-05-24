package com.tissue.feature.project.web;

import com.tissue.feature.project.application.dto.response.ProjectMemberSummary;
import com.tissue.feature.project.application.port.usecase.ProjectMemberQueryUseCase;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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

@Tag(name = "Project Member")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/members")
@RequiredArgsConstructor
public class ProjectMemberQueryController {

    private final ProjectMemberQueryUseCase projectMemberQueryUseCase;

    @Operation(operationId = "listProjectMembers", summary = "List project members", description = """
                    List members of a project. Can filter by role and keyword (matches \
                    username and display name, case-insensitive).

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Members retrieved"),
        @ApiResponse(responseCode = "404", description = "Project or member not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND, ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping
    public ResponseEntity<Page<ProjectMemberSummary>> listProjectMembers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestParam(required = false) @Nullable ProjectRole role,
            @RequestParam(required = false) @Nullable String keyword,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<ProjectMemberSummary> response = projectMemberQueryUseCase.getProjectMembers(
                ProjectIdentifier.of(workspaceKey, projectKey), role, keyword, pageable, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
