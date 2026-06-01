package com.tissue.feature.sprint.adapter.web;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.sprint.adapter.web.request.AddSprintIssuesRequest;
import com.tissue.feature.sprint.adapter.web.request.CreateSprintRequest;
import com.tissue.feature.sprint.adapter.web.request.MigrateIssuesRequest;
import com.tissue.feature.sprint.adapter.web.request.RemoveSprintIssuesRequest;
import com.tissue.feature.sprint.adapter.web.request.StartSprintRequest;
import com.tissue.feature.sprint.adapter.web.request.UpdateSprintRequest;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.feature.sprint.application.port.usecase.SprintCommandUseCase;
import com.tissue.feature.sprint.domain.exception.SprintErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.SprintErrors;
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

@Tag(name = "Sprint")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SprintCommandController {

    private final SprintCommandUseCase sprintCommandUseCase;

    @Operation(operationId = "createSprint", summary = "Create sprint", description = """
                Create a new sprint within a project.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sprint created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_NOT_FOUND,
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("projects/{projectKey}/sprints")
    public ResponseEntity<SprintCommandResult> createSprint(
            @PathVariable String projectKey,
            @RequestBody @Valid CreateSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        SprintCommandResult response = sprintCommandUseCase.createSprint(
                ProjectIdentifier.ofProjectKey(projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateSprint", summary = "Update sprint", description = """
                Update a sprint's name, goal, or description. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.INVALID_SPRINT_PERIOD,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PatchMapping("sprints/{sprintId}")
    public ResponseEntity<Void> updateSprint(
            @PathVariable Long sprintId,
            @RequestBody @Valid UpdateSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        sprintCommandUseCase.updateSprint(sprintId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "startSprint", summary = "Start sprint", description = """
                Start a sprint with a due date. Only sprints in `PLANNED` status can be started.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint started"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content),
        @ApiResponse(responseCode = "409", description = "Resource conflict", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.SPRINT_ALREADY_CLOSED,
        SprintErrorCode.ACTIVE_SPRINT_ALREADY_EXISTS,
        SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION,
        SprintErrorCode.INVALID_SPRINT_PERIOD,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("sprints/{sprintId}:start")
    public ResponseEntity<Void> startSprint(
            @PathVariable Long sprintId,
            @RequestBody @Valid StartSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.start(sprintId, request.dueAt(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "completeSprint", summary = "Complete sprint", description = """
                Complete an active sprint. Only sprints in `ACTIVE` status can be completed.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint completed"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.INCOMPLETE_SPRINT_ISSUES_FOUND,
        SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("sprints/{sprintId}:complete")
    public ResponseEntity<Void> completeSprint(
            @PathVariable Long sprintId, @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.complete(sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addSprintIssues", summary = "Add issues to sprint", description = """
                Add one or more issues to a sprint by their issue keys.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issues added to sprint"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.SPRINT_ALREADY_CLOSED,
        SprintErrorCode.SPRINT_ISSUE_PROJECT_MISMATCH,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("sprints/{sprintId}/issues")
    public ResponseEntity<Void> addSprintIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid AddSprintIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.addIssues(sprintId, request.issueKeys(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "migrateSprintIssues", summary = "Migrate incomplete issues", description = """
                Migrate incomplete issues from a completed sprint to another sprint.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issues migrated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.SPRINT_ALREADY_CLOSED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("sprints/{sprintId}:migrateIssues")
    public ResponseEntity<Void> migrateSprintIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid MigrateIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        sprintCommandUseCase.migrateIssues(sprintId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "removeSprintIssues", summary = "Remove issues from sprint", description = """
                Remove one or more issues from a sprint by their issue keys.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issues removed from sprint"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.SPRINT_ALREADY_CLOSED,
        SprintErrorCode.SPRINT_ISSUE_PROJECT_MISMATCH,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @DeleteMapping("sprints/{sprintId}/issues")
    public ResponseEntity<Void> removeSprintIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid RemoveSprintIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.removeIssues(sprintId, request.issueKeys(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "cancelSprint", summary = "Cancel sprint", description = """
                Cancel a sprint. Sprints in `PLANNING` or `ACTIVE` status can be cancelled.
                All issues in the sprint will be unassigned.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint cancelled"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.INVALID_SPRINT_STATUS_TRANSITION,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
        ProjectErrorCode.PROJECT_ARCHIVED,
    })
    @PostMapping("sprints/{sprintId}:cancel")
    public ResponseEntity<Void> cancelSprint(@PathVariable Long sprintId, @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.cancelSprint(sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteSprint", summary = "Delete sprint", description = """
                Delete a cancelled sprint. Only sprints in `CANCELLED` status can be deleted.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint deleted"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @SprintErrors({
        SprintErrorCode.SPRINT_NOT_FOUND,
        SprintErrorCode.SPRINT_NOT_CANCELLED,
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @DeleteMapping("sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long sprintId, @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.deleteSprint(sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
