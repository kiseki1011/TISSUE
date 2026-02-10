package com.tissue.project.web;

import com.tissue.feature.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.feature.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.feature.project.application.port.in.ProjectMemberUseCase;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.project.web.request.AddProjectMembersRequest;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/projects/{projectKey}/members")
public class ProjectMemberController {

    private final ProjectMemberUseCase commandUseCase;

    @PostMapping("/batch")
    public ResponseEntity<ProjectMembersCommandResult> addMembers(
            @PathVariable String projectKey,
            @RequestBody @Valid AddProjectMembersRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        ProjectMembersCommandResult response =
                commandUseCase.addMembers(projectKey, request.targetMemberIds(), currentWorkspaceMember);

        // TODO: use created?

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<ProjectMemberCommandResult> joinProjectDirectly(
            @PathVariable String projectKey, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        ProjectMemberCommandResult response = commandUseCase.join(projectKey, currentWorkspaceMember);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable String projectKey,
            @PathVariable Long memberId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        commandUseCase.kickMember(projectKey, memberId, currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> leaveProject(
            @PathVariable String projectKey, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        commandUseCase.leave(projectKey, currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }
}
