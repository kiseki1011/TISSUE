package com.tissue.feature.sprint.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.dto.response.SprintSummary;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.domain.SprintStatus;
import com.tissue.feature.sprint.domain.exception.SprintErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.SprintErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import com.tissue.shared.dto.ProjectIdentifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Set;
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

@Tag(name = "Sprint")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class SprintQueryController {

    private final SprintQueryUseCase sprintQueryUseCase;

    @Operation(operationId = "getSprint", summary = "Get sprint detail", description = """
                Get a single sprint with its full detail.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({SprintErrorCode.SPRINT_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("sprints/{sprintId}")
    public ResponseEntity<SprintDetail> getSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        SprintDetail response = sprintQueryUseCase.getSprintDetail(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listSprintIssueKeys", summary = "List sprint issue keys", description = """
                    List issue keys assigned to a sprint.

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint issue keys retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({SprintErrorCode.SPRINT_NOT_FOUND})
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("sprints/{sprintId}/issues")
    public ResponseEntity<SprintIssueKeys> listSprintIssueKeys(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        SprintIssueKeys response =
                sprintQueryUseCase.getSprintIssueKeys(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "listProjectSprints", summary = "List project sprints", description = """
                    List sprints of a project. Optional `statuses` filter accepts a comma separated \
                    set of sprint statuses (example: `statuses=ACTIVE,PLANNING`).

                    **Requirements:**
                    - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Project sprints retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_NOT_FOUND, ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @GetMapping("projects/{projectKey}/sprints")
    public ResponseEntity<Page<SprintSummary>> listProjectSprints(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestParam(required = false) @Nullable Set<SprintStatus> statuses,
            Pageable pageable,
            @CurrentMember MemberDetails memberDetails) {
        Page<SprintSummary> response = sprintQueryUseCase.getProjectSprints(
                ProjectIdentifier.of(workspaceKey, projectKey), statuses, pageable, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
