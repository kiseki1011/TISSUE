package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberQueryUseCase;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.global.openapi.WorkspaceErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Workspace Member")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
@RequiredArgsConstructor
public class WorkspaceMemberQueryController {

    private final WorkspaceMemberQueryUseCase workspaceMemberQueryUseCase;

    @Operation(operationId = "searchWorkspaceMembers", summary = "Search workspace members", description = """
                    Search workspace members by name or username. Optional `projectKey` filter limits results \
                    to members of the specified project.

                    **Requirements:**
                    - Requires workspace membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Search results retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @WorkspaceErrors({
        WorkspaceErrorCode.WORKSPACE_MEMBER_NOT_FOUND,
    })
    @GetMapping("/search")
    public ResponseEntity<List<WorkspaceMemberSearchResponse>> searchWorkspaceMembers(
            @PathVariable String workspaceKey,
            @Parameter(description = "Search keyword for name or username") @RequestParam String query,
            @Parameter(description = "Filter by project membership") @RequestParam(required = false) @Nullable
                    String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        return ResponseEntity.ok(workspaceMemberQueryUseCase.searchMembers(
                workspaceKey, projectKey, query, memberDetails.getMemberId()));
    }
}
