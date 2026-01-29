package com.tissue.workspace.adapter.in.web;

import com.tissue.workspace.adapter.in.web.request.UpdateDisplayNameRequest;
import com.tissue.workspace.adapter.in.web.request.UpdateRoleRequest;
import com.tissue.workspace.adapter.in.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.ManagePositionCommand;
import com.tissue.workspace.application.dto.request.ManageTeamCommand;
import com.tissue.workspace.application.dto.request.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.request.UpdateRoleCommand;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberManageUseCase workspaceMemberManageUseCase;
    private final WorkspaceMemberQueryUseCase workspaceMemberQueryUseCase;

    @PatchMapping("/{memberId}/displayName")
    public ResponseEntity<Void> updateDisplayName(
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateDisplayNameRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new UpdateDisplayNameCommand(memberId, request.displayName(), currentWorkspaceMember);
        workspaceMemberManageUseCase.updateDisplayName(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new UpdateRoleCommand(memberId, request.role(), currentWorkspaceMember);
        workspaceMemberManageUseCase.updateRole(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> addPosition(
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new ManagePositionCommand(memberId, positionId, currentWorkspaceMember);
        workspaceMemberManageUseCase.addPosition(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> removePosition(
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new ManagePositionCommand(memberId, positionId, currentWorkspaceMember);
        workspaceMemberManageUseCase.removePosition(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> addTeam(
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new ManageTeamCommand(memberId, teamId, currentWorkspaceMember);
        workspaceMemberManageUseCase.addTeam(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> removeTeam(
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new ManageTeamCommand(memberId, teamId, currentWorkspaceMember);
        workspaceMemberManageUseCase.removeTeam(command);

        return ResponseEntity.noContent().build();
    }
}
