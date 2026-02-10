package com.tissue.sprint.web;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.sprint.application.dto.response.SprintCommandResult;
import com.tissue.feature.sprint.application.dto.response.SprintDetail;
import com.tissue.feature.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.feature.sprint.application.port.in.SprintCommandUseCase;
import com.tissue.feature.sprint.application.port.in.SprintQueryUseCase;
import com.tissue.project.web.resolver.CurrentProjectMember;
import com.tissue.sprint.web.request.AddSprintIssuesRequest;
import com.tissue.sprint.web.request.CreateSprintRequest;
import com.tissue.sprint.web.request.MigrateIssuesRequest;
import com.tissue.sprint.web.request.RemoveSprintIssuesRequest;
import com.tissue.sprint.web.request.StartSprintRequest;
import com.tissue.sprint.web.request.UpdateSprintRequest;
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
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/sprints")
public class SprintController {

    private final SprintCommandUseCase sprintCommandUseCase;
    private final SprintQueryUseCase sprintQueryUseCase;

    @PostMapping
    public ResponseEntity<SprintCommandResult> createSprint(
            @RequestBody @Valid CreateSprintRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand();
        SprintCommandResult response = sprintCommandUseCase.createSprint(command, currentProjectMember);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{sprintId}")
    public ResponseEntity<Void> updateSprint(
            @PathVariable Long sprintId,
            @RequestBody @Valid UpdateSprintRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand();
        sprintCommandUseCase.updateSprint(sprintId, command, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/start")
    public ResponseEntity<Void> startSprint(
            @PathVariable Long sprintId,
            @RequestBody @Valid StartSprintRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        sprintCommandUseCase.start(sprintId, request.dueAt(), currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<Void> completeSprint(
            @PathVariable Long sprintId, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        sprintCommandUseCase.complete(sprintId, currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/issues")
    public ResponseEntity<Void> addIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid AddSprintIssuesRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        sprintCommandUseCase.addIssues(sprintId, request.issueKeys(), currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/issues/migrate")
    public ResponseEntity<Void> migrateIncompleteIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid MigrateIssuesRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand();
        sprintCommandUseCase.migrateIssues(sprintId, command, currentProjectMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sprintId}/issues")
    public ResponseEntity<Void> removeIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid RemoveSprintIssuesRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        sprintCommandUseCase.removeIssues(sprintId, request.issueKeys(), currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{sprintId}")
    public ResponseEntity<SprintDetail> getSprintDetail(
            @PathVariable Long sprintId, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        SprintDetail response = sprintQueryUseCase.getSprintDetail(sprintId, currentProjectMember);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{sprintId}/issues")
    public ResponseEntity<SprintIssueKeys> getSprintIssueKeys(
            @PathVariable Long sprintId, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        SprintIssueKeys response = sprintQueryUseCase.getSprintIssueKeys(sprintId, currentProjectMember);
        return ResponseEntity.ok(response);
    }
}
