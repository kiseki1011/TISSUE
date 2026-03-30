package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceUseCase;
import com.tissue.feature.workspace.web.request.CreateWorkspaceRequest;
import com.tissue.feature.workspace.web.request.UpdateWorkspaceInfoRequest;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceUseCase workspaceUseCase;

    @PostMapping
    public ResponseEntity<WorkspaceCreateResponse> createWorkspace(
            @RequestBody @Valid CreateWorkspaceRequest request, @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        WorkspaceCreateResponse response = workspaceUseCase.create(command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{workspaceKey}")
                .buildAndExpand(response.workspaceKey())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{workspaceKey}")
    public ResponseEntity<Void> updateWorkspaceInfo(
            @PathVariable String workspaceKey,
            @RequestBody @Valid UpdateWorkspaceInfoRequest request,
            @CurrentMember MemberDetails memberDetails) {
        var command = request.toCommand();
        workspaceUseCase.update(workspaceKey, command, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workspaceKey}")
    public ResponseEntity<Void> delete(@PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.delete(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workspaceKey}/members/{targetMemberId}/ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable String workspaceKey,
            @PathVariable Long targetMemberId,
            @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.transferOwnership(workspaceKey, targetMemberId, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceKey}")
    public ResponseEntity<WorkspaceDetail> getWorkspaceDetail(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        WorkspaceDetail response = workspaceUseCase.getDetail(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<WorkspaceSummaryResponse>> listMyWorkspaces(@CurrentMember MemberDetails userDetails) {
        List<WorkspaceSummaryResponse> response = workspaceUseCase.getMyWorkspaces(userDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{workspaceKey}/archive")
    public ResponseEntity<Void> archive(@PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.archive(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workspaceKey}/unarchive")
    public ResponseEntity<Void> restoreArchived(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.restoreArchived(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workspaceKey}/restore")
    public ResponseEntity<Void> restoreDeleted(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {
        workspaceUseCase.restoreDeleted(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<DeletedWorkspaceSummary>> listMyDeletedWorkspaces(
            @CurrentMember MemberDetails memberDetails) {
        List<DeletedWorkspaceSummary> response = workspaceUseCase.getMyDeletedWorkspaces(memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
