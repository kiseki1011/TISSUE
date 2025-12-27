package com.tissue.workspace.adapter.in.web;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateDisplayNameRequest;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateRoleRequest;
import com.tissue.workspace.application.dto.in.ManagePositionCommand;
import com.tissue.workspace.application.dto.in.ManageTeamCommand;
import com.tissue.workspace.application.dto.in.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.in.UpdateRoleCommand;
import com.tissue.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.application.port.in.WorkspaceMemberQueryUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
public class WorkspaceMemberController {

    private final WorkspaceMemberManageUseCase workspaceMemberManageUseCase;
    private final WorkspaceMemberQueryUseCase workspaceMemberQueryUseCase;

    @PatchMapping("/{memberId}/displayName")
    public ResponseEntity<Void> updateDisplayName(
            @PathVariable String workspaceKey,
            @RequestBody @Valid UpdateDisplayNameRequest request,
            @CurrentMember MemberUserDetails userDetails) {
        var command =
                new UpdateDisplayNameCommand(
                        workspaceKey, userDetails.getMemberId(), request.displayName());
        workspaceMemberManageUseCase.updateDisplayName(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @CurrentMember MemberUserDetails userDetails) {
        var command =
                UpdateRoleCommand.builder()
                        .workspaceKey(workspaceKey)
                        .memberId(memberId)
                        .role(request.role())
                        .actorMemberId(userDetails.getMemberId())
                        .build();
        workspaceMemberManageUseCase.updateRole(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> addPosition(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentMember MemberUserDetails userDetails) {
        var command =
                ManagePositionCommand.builder()
                        .workspaceKey(workspaceKey)
                        .positionId(positionId)
                        .memberId(memberId)
                        .actorMemberId(userDetails.getMemberId())
                        .build();
        workspaceMemberManageUseCase.addPosition(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> removePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentMember MemberUserDetails userDetails) {
        var command =
                ManagePositionCommand.builder()
                        .workspaceKey(workspaceKey)
                        .positionId(positionId)
                        .memberId(memberId)
                        .actorMemberId(userDetails.getMemberId())
                        .build();
        workspaceMemberManageUseCase.removePosition(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> addTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentMember MemberUserDetails userDetails) {
        var command =
                ManageTeamCommand.builder()
                        .workspaceKey(workspaceKey)
                        .teamId(teamId)
                        .memberId(memberId)
                        .actorMemberId(userDetails.getMemberId())
                        .build();
        workspaceMemberManageUseCase.addTeam(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> removeTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentMember MemberUserDetails userDetails) {
        var command =
                ManageTeamCommand.builder()
                        .workspaceKey(workspaceKey)
                        .teamId(teamId)
                        .memberId(memberId)
                        .actorMemberId(userDetails.getMemberId())
                        .build();
        workspaceMemberManageUseCase.removeTeam(command);

        return ResponseEntity.noContent().build();
    }
}
