package com.tissue.workspace.adapter.in.web;

import com.tissue.workspace.adapter.in.web.annotation.CurrentWorkspaceMember;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateDisplayNameRequest;
import com.tissue.workspace.adapter.in.web.dto.request.UpdateRoleRequest;
import com.tissue.workspace.application.dto.in.ManagePositionCommand;
import com.tissue.workspace.application.dto.in.ManageTeamCommand;
import com.tissue.workspace.application.dto.in.UpdateDisplayNameCommand;
import com.tissue.workspace.application.dto.in.UpdateRoleCommand;
import com.tissue.workspace.application.dto.info.WorkspaceMemberInfo;
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
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateDisplayNameRequest request,
            @CurrentWorkspaceMember WorkspaceMemberInfo currentWorkspaceMember) {

        var command =
                new UpdateDisplayNameCommand(workspaceKey, memberId, request.displayName(), currentWorkspaceMember);
        workspaceMemberManageUseCase.updateDisplayName(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @CurrentWorkspaceMember WorkspaceMemberInfo currentWorkspaceMember) {

        var command = new UpdateRoleCommand(workspaceKey, memberId, request.role(), currentWorkspaceMember);
        workspaceMemberManageUseCase.updateRole(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> addPosition(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentWorkspaceMember WorkspaceMemberInfo currentWorkspaceMember) {

        var command = new ManagePositionCommand(workspaceKey, memberId, positionId, currentWorkspaceMember);
        workspaceMemberManageUseCase.addPosition(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> removePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentWorkspaceMember WorkspaceMemberInfo currentWorkspaceMember) {

        var command = new ManagePositionCommand(workspaceKey, memberId, positionId, currentWorkspaceMember);
        workspaceMemberManageUseCase.removePosition(command);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> addTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentWorkspaceMember WorkspaceMemberInfo currentWorkspaceMember) {

        var command = new ManageTeamCommand(workspaceKey, memberId, teamId, currentWorkspaceMember);
        workspaceMemberManageUseCase.addTeam(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> removeTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentWorkspaceMember WorkspaceMemberInfo currentWorkspaceMember) {

        var command = new ManageTeamCommand(workspaceKey, memberId, teamId, currentWorkspaceMember);
        workspaceMemberManageUseCase.removeTeam(command);

        return ResponseEntity.noContent().build();
    }
}
