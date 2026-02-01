package com.tissue.workspace.adapter.web;

import com.tissue.global.security.principal.MemberDetails;
import com.tissue.workspace.adapter.web.request.CreateWorkspaceRequest;
import com.tissue.workspace.adapter.web.request.UpdateWorkspaceInfoRequest;
import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.TransferOwnershipCommand;
import com.tissue.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.workspace.application.port.in.WorkspaceCommandUseCase;
import com.tissue.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.workspace.application.port.in.WorkspaceQueryUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final WorkspaceCreateUseCase workspaceCreateUseCase;
    private final WorkspaceCommandUseCase workspaceCommandUseCase;
    private final WorkspaceQueryUseCase workspaceQueryUseCase;

    @PostMapping
    public ResponseEntity<WorkspaceCreateResponse> createWorkspace(
        @RequestBody @Valid CreateWorkspaceRequest request,
        @AuthenticationPrincipal MemberDetails userDetails) {

        var command = request.toCommand(userDetails.getMemberId());
        WorkspaceCreateResponse response = workspaceCreateUseCase.create(command);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                                                  .path("/{workspaceKey}")
                                                  .buildAndExpand(response.workspaceKey())
                                                  .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @PatchMapping("/{workspaceKey}")
    public ResponseEntity<Void> updateWorkspaceInfo(
        @RequestBody @Valid UpdateWorkspaceInfoRequest request,
        @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = request.toCommand(currentWorkspaceMember);
        workspaceCommandUseCase.updateInfo(command);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workspaceKey}")
    public ResponseEntity<Void> delete(
        @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {
        workspaceCommandUseCase.delete(currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workspaceKey}/members/{memberId}/ownership")
    public ResponseEntity<Void> transferOwnership(
        @PathVariable Long memberId,
        @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = new TransferOwnershipCommand(memberId, currentWorkspaceMember);
        workspaceCommandUseCase.transferOwnership(command);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceKey}")
    public ResponseEntity<WorkspaceDetail> getWorkspaceDetail(
        @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {
        WorkspaceDetail response = workspaceQueryUseCase.getDetail(currentWorkspaceMember);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<WorkspaceSummaryResponse>> listMyWorkspaces(
        @AuthenticationPrincipal MemberDetails userDetails) {
        List<WorkspaceSummaryResponse> response = workspaceQueryUseCase.getMyWorkspaces(
            userDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
