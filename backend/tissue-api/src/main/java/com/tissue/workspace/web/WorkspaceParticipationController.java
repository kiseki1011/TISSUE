package com.tissue.workspace.web;

import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceParticipationUseCase;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import com.tissue.workspace.web.request.InviteToWorkspaceRequest;
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
            @PathVariable String workspaceKey,
            @RequestBody @Valid InviteToWorkspaceRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        InviteMembersResponse response =
                workspaceParticipationUseCase.inviteToWorkspace(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> leaveWorkspace(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        workspaceParticipationUseCase.leave(workspaceKey, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetMemberId}")
    public ResponseEntity<Void> kickWorkspaceMember(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {

        workspaceParticipationUseCase.kick(workspaceKey, targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
