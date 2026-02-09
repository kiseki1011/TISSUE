package com.tissue.workspace.web;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.workspace.application.port.in.WorkspaceParticipationUseCase;
import com.tissue.workspace.web.request.InviteToWorkspaceRequest;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
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

        var command = request.toCommand();
        InviteMembersResponse response = workspaceParticipationUseCase.inviteToWorkspace(command, actorContext);

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

        workspaceParticipationUseCase.kick(memberId, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }
}
