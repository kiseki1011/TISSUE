package com.tissue.project.adapter.in.web;

import com.tissue.project.adapter.in.web.dto.request.AddProjectMembersRequest;
import com.tissue.project.adapter.in.web.dto.request.ChangeProjectRoleRequest;
import com.tissue.project.application.dto.request.ChangeProjectRoleCommand;
import com.tissue.project.application.dto.request.DirectJoinProjectCommand;
import com.tissue.project.application.dto.request.KickProjectMemberCommand;
import com.tissue.project.application.dto.response.ProjectMemberCommandResult;
import com.tissue.project.application.dto.response.ProjectMembersCommandResult;
import com.tissue.project.application.port.in.ProjectMemberCommandUseCase;
import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
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

    private final ProjectMemberCommandUseCase commandUseCase;

    @PostMapping("/batch")
    public ResponseEntity<ProjectMembersCommandResult> addMembers(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid AddProjectMembersRequest request) {
        var command = request.toCommand(workspaceKey, projectKey);
        ProjectMembersCommandResult response = commandUseCase.addMembers(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping
    public ResponseEntity<ProjectMemberCommandResult> joinProjectDirectly(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberUserDetails currentMember) {
        var command = new DirectJoinProjectCommand(workspaceKey, projectKey, currentMember.getMemberId());
        ProjectMemberCommandResult response = commandUseCase.joinViaDirect(command);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping
    public ResponseEntity<ProjectMemberCommandResult> leaveProject(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @CurrentMember MemberUserDetails currentMember) {
        ProjectMemberCommandResult response =
                commandUseCase.leave(workspaceKey, projectKey, currentMember.getMemberId());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<ProjectMemberCommandResult> kickMember(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @PathVariable Long memberId,
            @CurrentMember MemberUserDetails currentMember) {
        var command = KickProjectMemberCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .targetMemberId(memberId)
                .actorMemberId(currentMember.getMemberId())
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
            @CurrentMember MemberUserDetails currentMember) {
        var command = ChangeProjectRoleCommand.builder()
                .workspaceKey(workspaceKey)
                .projectKey(projectKey)
                .newRole(request.newProjectRole())
                .targetMemberId(memberId)
                .actorMemberId(currentMember.getMemberId())
                .build();

        ProjectMemberCommandResult response = commandUseCase.changeProjectRole(command);

        return ResponseEntity.ok(response);
    }
}
