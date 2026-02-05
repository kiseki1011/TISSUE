package com.tissue.project.adapter.web;

import com.tissue.project.adapter.web.request.AddProjectMembersRequest;
import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectMemberUseCase;
import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
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
            @RequestBody @Valid AddProjectMembersRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        ProjectMembersCommandResult response =
                commandUseCase.addMembers(request.targetMemberIds(), currentProjectMember);

        // TODO: use created?

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<ProjectMemberCommandResult> joinProjectDirectly(
            @PathVariable String projectKey, @CurrentWorkspaceMember WorkspaceMemberContext workspaceMemberContext) {

        ProjectMemberCommandResult response = commandUseCase.join(projectKey, workspaceMemberContext);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> kickMember(
            @PathVariable Long memberId, @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        commandUseCase.kickMember(memberId, currentProjectMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> leaveProject(@CurrentProjectMember ProjectMemberContext currentProjectMember) {
        commandUseCase.leave(currentProjectMember);
        return ResponseEntity.noContent().build();
    }
}
