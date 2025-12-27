package com.tissue.project.adapter.in.web;

import com.tissue.project.adapter.in.web.dto.request.CreateProjectRequest;
import com.tissue.project.adapter.in.web.dto.request.UpdateProjectRequest;
import com.tissue.project.application.dto.request.DeleteProjectCommand;
import com.tissue.project.application.dto.response.ProjectCommandResult;
import com.tissue.project.application.port.in.ProjectCommandUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects")
public class ProjectController {

    private final ProjectCommandUseCase projectCommandUseCase;

    @PostMapping
    public ResponseEntity<ProjectCommandResult> create(
            @PathVariable String workspaceKey, @RequestBody @Valid CreateProjectRequest request) {
        var command = request.toCommand(workspaceKey);
        ProjectCommandResult response = projectCommandUseCase.create(command);

        URI location =
                ServletUriComponentsBuilder.fromCurrentRequest()
                        .path("/{projectKey}")
                        .buildAndExpand(response.projectKey())
                        .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{projectKey}")
    public ResponseEntity<ProjectCommandResult> update(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid UpdateProjectRequest request) {
        var command = request.toCommand(workspaceKey, projectKey);
        ProjectCommandResult response = projectCommandUseCase.update(command);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectKey}")
    public ResponseEntity<ProjectCommandResult> delete(
            @PathVariable String workspaceKey, @PathVariable String projectKey) {
        var command = new DeleteProjectCommand(workspaceKey, projectKey);
        ProjectCommandResult response = projectCommandUseCase.delete(command);

        return ResponseEntity.ok(response);
    }
}
