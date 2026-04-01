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

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
@RequiredArgsConstructor
public class SprintController {

    private final SprintCommandUseCase sprintCommandUseCase;
    private final SprintQueryUseCase sprintQueryUseCase;

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

    @PatchMapping("sprints/{sprintId}/start")
    public ResponseEntity<Void> startSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid StartSprintRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.start(workspaceKey, sprintId, request.dueAt(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("sprints/{sprintId}/complete")
    public ResponseEntity<Void> completeSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.complete(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("sprints/{sprintId}/issues")
    public ResponseEntity<Void> addIssues(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid AddSprintIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.addIssues(workspaceKey, sprintId, request.issueKeys(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("sprints/{sprintId}/issues/migrate")
    public ResponseEntity<Void> migrateIncompleteIssues(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid MigrateIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        sprintCommandUseCase.migrateIssues(workspaceKey, sprintId, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("sprints/{sprintId}/issues")
    public ResponseEntity<Void> removeIssues(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @RequestBody @Valid RemoveSprintIssuesRequest request,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.removeIssues(workspaceKey, sprintId, request.issueKeys(), memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("sprints/{sprintId}")
    public ResponseEntity<Void> deleteSprint(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        sprintCommandUseCase.deleteSprint(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("sprints/{sprintId}")
    public ResponseEntity<SprintDetail> getSprintDetail(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        SprintDetail response = sprintQueryUseCase.getSprintDetail(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("sprints/{sprintId}/issues")
    public ResponseEntity<SprintIssueKeys> getSprintIssueKeys(
            @PathVariable String workspaceKey,
            @PathVariable Long sprintId,
            @CurrentMember MemberDetails memberDetails) {
        SprintIssueKeys response =
                sprintQueryUseCase.getSprintIssueKeys(workspaceKey, sprintId, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
