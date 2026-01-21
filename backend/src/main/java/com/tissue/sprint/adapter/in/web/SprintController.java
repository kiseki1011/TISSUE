package com.tissue.sprint.adapter.in.web;

import com.tissue.project.adapter.in.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.sprint.adapter.in.web.dto.request.AddSprintIssuesRequest;
import com.tissue.sprint.adapter.in.web.dto.request.CreateSprintRequest;
import com.tissue.sprint.adapter.in.web.dto.request.MigrateIssuesRequest;
import com.tissue.sprint.adapter.in.web.dto.request.RemoveSprintIssuesRequest;
import com.tissue.sprint.adapter.in.web.dto.request.StartSprintRequest;
import com.tissue.sprint.adapter.in.web.dto.request.UpdateSprintRequest;
import com.tissue.sprint.application.dto.request.AddSprintIssuesCommand;
import com.tissue.sprint.application.dto.request.CompleteSprintCommand;
import com.tissue.sprint.application.dto.request.RemoveSprintIssuesCommand;
import com.tissue.sprint.application.dto.response.SprintCommandResult;
import com.tissue.sprint.application.dto.response.SprintDetail;
import com.tissue.sprint.application.dto.response.SprintIssueKeys;
import com.tissue.sprint.application.port.in.SprintCommandUseCase;
import com.tissue.sprint.application.port.in.SprintQueryUseCase;
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

        var command = request.toCommand(currentProjectMember);
        SprintCommandResult response = sprintCommandUseCase.createSprint(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{sprintId}")
    public ResponseEntity<Void> updateSprint(
            @PathVariable Long sprintId,
            @RequestBody @Valid UpdateSprintRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(sprintId, currentProjectMember);
        sprintCommandUseCase.updateSprint(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/start")
    public ResponseEntity<Void> startSprint(
            @PathVariable Long sprintId,
            @RequestBody @Valid StartSprintRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(sprintId, currentProjectMember);
        sprintCommandUseCase.start(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/complete")
    public ResponseEntity<Void> completeSprint(
            @PathVariable Long sprintId, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new CompleteSprintCommand(sprintId, currentProjectMember);
        sprintCommandUseCase.complete(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/issues")
    public ResponseEntity<Void> addIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid AddSprintIssuesRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new AddSprintIssuesCommand(sprintId, request.issueKeys(), currentProjectMember);
        sprintCommandUseCase.addIssues(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{sprintId}/issues/migrate")
    public ResponseEntity<Void> migrateIncompleteIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid MigrateIssuesRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(sprintId, currentProjectMember);
        sprintCommandUseCase.migrateIssues(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{sprintId}/issues")
    public ResponseEntity<Void> removeIssues(
            @PathVariable Long sprintId,
            @RequestBody @Valid RemoveSprintIssuesRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = new RemoveSprintIssuesCommand(sprintId, request.issueKeys(), currentProjectMember);
        sprintCommandUseCase.removeIssues(command);

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
