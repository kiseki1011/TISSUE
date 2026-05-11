package com.tissue.feature.sprint.web;

import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.port.usecase.SprintCommandUseCase;
import com.tissue.feature.sprint.application.port.usecase.SprintQueryUseCase;
import com.tissue.feature.sprint.web.request.AddSprintIssuesRequest;
import com.tissue.feature.sprint.web.request.CreateSprintRequest;
import com.tissue.feature.sprint.web.request.MigrateIssuesRequest;
import com.tissue.feature.sprint.web.request.RemoveSprintIssuesRequest;
import com.tissue.feature.sprint.web.request.StartSprintRequest;
import com.tissue.feature.sprint.web.request.UpdateSprintRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sprint")
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class SprintController {

    private final SprintCommandUseCase sprintCommandUseCase;
    private final SprintQueryUseCase sprintQueryUseCase;

    @Operation(operationId = "createSprint", summary = "Create sprint", description = """
                Create a new sprint within a project.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Sprint created"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content)
    })
    @PostMapping("projects/{projectKey}/sprints")
    public ResponseEntity<SprintCommandResult> createSprint(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        SprintCommandResult response = sprintCommandUseCase.createSprint(
                ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(operationId = "updateSprint", summary = "Update sprint", description = """
                Update a sprint's name, goal, or description. Only provided fields are updated.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint updated"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @PatchMapping("sprints/{sprintId}")
    public ResponseEntity<Void> updateSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid UpdateSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        sprintCommandUseCase.updateSprint(workspaceKey, sprintId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "startSprint", summary = "Start sprint", description = """
                Start a sprint with a due date. Only sprints in `PLANNED` status can be started.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint started"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition or request", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @PostMapping("sprints/{sprintId}:start")
    public ResponseEntity<Void> startSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid StartSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.start(workspaceKey, sprintId, request.dueAt(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "completeSprint", summary = "Complete sprint", description = """
                Complete an active sprint. Only sprints in `ACTIVE` status can be completed.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint completed"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @PostMapping("sprints/{sprintId}:complete")
    public ResponseEntity<Void> completeSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.complete(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "addSprintIssues", summary = "Add issues to sprint", description = """
                Add one or more issues to a sprint by their issue keys.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issues added to sprint"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint or issue not found", content = @Content)
    })
    @PostMapping("sprints/{sprintId}/issues")
    public ResponseEntity<Void> addSprintIssues(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid AddSprintIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.addIssues(workspaceKey, sprintId, request.issueKeys(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "migrateSprintIssues", summary = "Migrate incomplete issues", description = """
                Migrate incomplete issues from a completed sprint to another sprint.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issues migrated"),
        @ApiResponse(responseCode = "400", description = "Invalid request or sprint status", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @PostMapping("sprints/{sprintId}:migrateIssues")
    public ResponseEntity<Void> migrateSprintIssues(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid MigrateIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        sprintCommandUseCase.migrateIssues(workspaceKey, sprintId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "removeSprintIssues", summary = "Remove issues from sprint", description = """
                Remove one or more issues from a sprint by their issue keys.""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Issues removed from sprint"),
        @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @DeleteMapping("sprints/{sprintId}/issues")
    public ResponseEntity<Void> removeSprintIssues(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid RemoveSprintIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.removeIssues(workspaceKey, sprintId, request.issueKeys(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "cancelSprint", summary = "Cancel sprint", description = """
                Cancel a sprint. Sprints in `PLANNING` or `ACTIVE` status can be cancelled.
                All issues in the sprint will be unassigned.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint cancelled"),
        @ApiResponse(responseCode = "400", description = "Invalid status transition", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @PostMapping("sprints/{sprintId}:cancel")
    public ResponseEntity<Void> cancelSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.cancelSprint(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(operationId = "deleteSprint", summary = "Delete sprint", description = """
                Delete a cancelled sprint. Only sprints in `CANCELLED` status can be deleted.

                **Requirements:**
                - Requires project `MANAGER` or workspace `ADMIN` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Sprint deleted"),
        @ApiResponse(responseCode = "400", description = "Sprint is not in CANCELLED status", content = @Content),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @DeleteMapping("sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.deleteSprint(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @Operation(
            operationId = "getSprint",
            summary = "Get sprint detail",
            description = "Retrieve the full detail of a sprint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint detail retrieved"),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @GetMapping("sprints/{sprintId}")
    public ResponseEntity<SprintDetail> getSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        SprintDetail response = sprintQueryUseCase.getSprintDetail(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(
            operationId = "listSprintIssueKeys",
            summary = "Get sprint issue keys",
            description = "Retrieve all issue keys assigned to a sprint.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sprint issue keys retrieved"),
        @ApiResponse(responseCode = "404", description = "Sprint not found", content = @Content)
    })
    @GetMapping("sprints/{sprintId}/issues")
    public ResponseEntity<SprintIssueKeys> listSprintIssueKeys(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        SprintIssueKeys response =
                sprintQueryUseCase.getSprintIssueKeys(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
