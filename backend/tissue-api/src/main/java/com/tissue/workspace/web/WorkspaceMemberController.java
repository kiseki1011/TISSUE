package com.tissue.workspace.web;

import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.in.WorkspaceMemberManageUseCase;
import com.tissue.workspace.web.request.UpdateDisplayNameRequest;
import com.tissue.workspace.web.request.UpdateRoleRequest;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/members")
@RequiredArgsConstructor
public class WorkspaceMemberController {

    private final WorkspaceMemberManageUseCase workspaceMemberManageUseCase;
    //    private final WorkspaceMemberQueryUseCase workspaceMemberQueryUseCase;

    @PatchMapping("/{memberId}/displayName")
    public ResponseEntity<Void> updateDisplayName(
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateDisplayNameRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceMemberManageUseCase.updateDisplayName(memberId, request.displayName(), currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable Long memberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceMemberManageUseCase.updateRole(memberId, request.role(), currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> addPosition(
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceMemberManageUseCase.addPosition(memberId, positionId, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/positions/{positionId}")
    public ResponseEntity<Void> removePosition(
            @PathVariable Long memberId,
            @PathVariable Long positionId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceMemberManageUseCase.removePosition(memberId, positionId, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> addTeam(
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceMemberManageUseCase.addTeam(memberId, teamId, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{memberId}/teams/{teamId}")
    public ResponseEntity<Void> removeTeam(
            @PathVariable Long memberId,
            @PathVariable Long teamId,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceMemberManageUseCase.removeTeam(memberId, teamId, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<WorkspaceMemberSearchResponse>> searchMembers(
            @PathVariable String workspaceKey,
            @CurrentWorkspaceMember WorkspaceMemberContext context,
            @RequestParam String query,
            @RequestParam(required = false) @Nullable String projectKey) {

        return ResponseEntity.ok(workspaceMemberManageUseCase.searchMembers(context, query, projectKey));
    }
}
