package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceMemberSearchResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceMemberManageUseCase;
import com.tissue.feature.workspace.web.request.UpdateDisplayNameRequest;
import com.tissue.feature.workspace.web.request.UpdateRoleRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
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

    @PatchMapping("/displayName")
    public ResponseEntity<Void> updateDisplayName(
            @PathVariable String workspaceKey,
            @RequestBody @Valid UpdateDisplayNameRequest request,
            @CurrentMember MemberDetails memberDetails) {

        workspaceMemberManageUseCase.updateDisplayName(
                workspaceKey, request.displayName(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{targetMemberId}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @RequestBody @Valid UpdateRoleRequest request,
            @CurrentMember MemberDetails memberDetails) {

        workspaceMemberManageUseCase.updateRole(
                workspaceKey, targetMemberId, request.role(), memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{targetMemberId}/positions/{positionId}")
    public ResponseEntity<Void> addPosition(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {

        workspaceMemberManageUseCase.addPosition(workspaceKey, targetMemberId, positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetMemberId}/positions/{positionId}")
    public ResponseEntity<Void> removePosition(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long positionId,
            @CurrentMember MemberDetails memberDetails) {

        workspaceMemberManageUseCase.removePosition(
                workspaceKey, targetMemberId, positionId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{targetMemberId}/teams/{teamId}")
    public ResponseEntity<Void> addTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long teamId,
            @CurrentMember MemberDetails memberDetails) {

        workspaceMemberManageUseCase.addTeam(workspaceKey, targetMemberId, teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetMemberId}/teams/{teamId}")
    public ResponseEntity<Void> removeTeam(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @PathVariable Long teamId,
            @CurrentMember MemberDetails memberDetails) {

        workspaceMemberManageUseCase.removeTeam(workspaceKey, targetMemberId, teamId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public ResponseEntity<List<WorkspaceMemberSearchResponse>> searchMembers(
            @PathVariable String workspaceKey,
            @RequestParam String query,
            @RequestParam(required = false) @Nullable String projectKey,
            @CurrentMember MemberDetails memberDetails) {

        return ResponseEntity.ok(workspaceMemberManageUseCase.searchMembers(
                workspaceKey, projectKey, query, memberDetails.getMemberId()));
    }
}
