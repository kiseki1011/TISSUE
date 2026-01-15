package com.tissue.project.adapter.in.web;

import com.tissue.project.adapter.in.web.dto.request.AddProjectMembersRequest;
import com.tissue.project.adapter.in.web.dto.request.ChangeProjectRoleRequest;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectParticipationUseCase;
import com.tissue.security.authentication.domain.MemberDetails;
import com.tissue.security.authentication.presentation.annotation.CurrentMember;
import com.tissue.workspace.adapter.in.web.annotation.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
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

    private final ProjectParticipationUseCase commandUseCase;

    @PostMapping("/batch")
    public ResponseEntity<ProjectMembersCommandResult> addMembers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid AddProjectMembersRequest request,
            @CurrentWorkspaceMember WorkspaceMemberInfo workspaceMemberInfo) {

        var command = request.toCommand(workspaceKey, projectKey, workspaceMemberInfo);
        ProjectMembersCommandResult response = commandUseCase.addMembers(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<ProjectMemberCommandResult> joinProjectDirectly(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentWorkspaceMember WorkspaceMemberInfo workspaceMemberInfo) {

        var command = new DirectJoinProjectCommand(workspaceKey, projectKey, workspaceMemberInfo);
        ProjectMemberCommandResult response = commandUseCase.joinViaDirect(command);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<ProjectMemberCommandResult> leaveProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberDetails currentMember) {

        ProjectMemberCommandResult response =
                commandUseCase.leave(workspaceKey, projectKey, currentMember.getMemberId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ProjectMemberCommandResult> kickMember(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long memberId,
            @CurrentWorkspaceMember WorkspaceMemberInfo workspaceMemberInfo) {

        var command = KickProjectMemberCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .targetMemberId(memberId)
                .actor(workspaceMemberInfo)
                .build();

        ProjectMemberCommandResult response = commandUseCase.kickMember(command);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<ProjectMemberCommandResult> changeProjectRole(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long memberId,
            @RequestBody @Valid ChangeProjectRoleRequest request,
            @CurrentWorkspaceMember WorkspaceMemberInfo workspaceMemberInfo) {

        var command = ChangeProjectRoleCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .newRole(request.newProjectRole())
                .targetMemberId(memberId)
                .actor(workspaceMemberInfo)
                .build();

        ProjectMemberCommandResult response = commandUseCase.changeProjectRole(command);
        return ResponseEntity.ok(response);
    }
}
