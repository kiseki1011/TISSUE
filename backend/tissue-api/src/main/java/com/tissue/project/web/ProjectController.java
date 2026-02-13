package com.tissue.project.web;

import com.tissue.feature.project.application.dto.response.ProjectResponse;
import com.tissue.feature.project.application.port.usecase.ProjectUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.project.web.request.CreateProjectRequest;
import com.tissue.project.web.request.UpdateProjectRequest;
import com.tissue.shared.dto.ProjectIdentifier;
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

    private final ProjectUseCase projectUseCase;

    @PostMapping
    public ResponseEntity<ProjectResponse> create(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateProjectRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        ProjectResponse response = projectUseCase.create(workspaceKey, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{projectKey}")
                .buildAndExpand(response.projectKey())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{projectKey}")
    public ResponseEntity<Void> update(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid UpdateProjectRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        projectUseCase.update(ProjectIdentifier.of(workspaceKey, projectKey), command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{projectKey}")
    public ResponseEntity<Void> delete(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        projectUseCase.delete(ProjectIdentifier.of(workspaceKey, projectKey), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
