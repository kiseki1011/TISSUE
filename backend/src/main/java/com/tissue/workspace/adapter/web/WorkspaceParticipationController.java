package com.tissue.workspace.adapter.web;

import com.tissue.project.adapter.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workspace.adapter.web.request.InviteToProjectRequest;
import com.tissue.workspace.adapter.web.request.InviteToWorkspaceRequest;
import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.KickWorkspaceMemberCommand;
import com.tissue.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.workspace.application.port.in.WorkspaceParticipationUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceParticipationController {

    private final WorkspaceParticipationUseCase workspaceParticipationUseCase;

    @PostMapping("/invitations")
    public ResponseEntity<InviteMembersResponse> inviteToWorkspace(
            @RequestBody @Valid InviteToWorkspaceRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext actorContext) {

        var command = request.toCommand(actorContext);
        InviteMembersResponse response = workspaceParticipationUseCase.inviteToWorkspace(command);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/projects/{projectKey}/invitations")
    public ResponseEntity<InviteMembersResponse> inviteToProject(
            @RequestBody @Valid InviteToProjectRequest request,
            @CurrentProjectMember ProjectMemberContext currentProjectMember) {

        var command = request.toCommand(currentProjectMember);
        InviteMembersResponse response = workspaceParticipationUseCase.inviteToProject(command);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leaveWorkspace(@CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceParticipationUseCase.leave(currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<Void> kickWorkspaceMember(
            @PathVariable Long memberId, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new KickWorkspaceMemberCommand(memberId, currentWorkspaceMember);
        workspaceParticipationUseCase.kick(command);

        return ResponseEntity.noContent().build();
    }
}
