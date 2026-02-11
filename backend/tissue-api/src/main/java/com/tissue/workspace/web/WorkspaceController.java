package com.tissue.workspace.web;

import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.feature.workspace.application.port.in.WorkspaceUseCase;
import com.tissue.principal.MemberDetails;
import com.tissue.workspace.web.request.CreateWorkspaceRequest;
import com.tissue.workspace.web.request.UpdateWorkspaceInfoRequest;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
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

    private final WorkspaceUseCase workspaceUseCase;

    @PostMapping
    public ResponseEntity<WorkspaceCreateResponse> createWorkspace(
            @RequestBody @Valid CreateWorkspaceRequest request, @AuthenticationPrincipal MemberDetails userDetails) {

        var command = request.toCommand();
        WorkspaceCreateResponse response = workspaceUseCase.create(command, userDetails.getMemberId());

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

        var command = request.toCommand();
        workspaceUseCase.update(command, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{workspaceKey}")
    public ResponseEntity<Void> delete(@CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceUseCase.delete(currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{workspaceKey}/members/{memberId}/ownership")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long memberId, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        workspaceUseCase.transferOwnership(memberId, currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{workspaceKey}")
    public ResponseEntity<WorkspaceDetail> getWorkspaceDetail(
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        WorkspaceDetail response = workspaceUseCase.getDetail(currentWorkspaceMember);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<List<WorkspaceSummaryResponse>> listMyWorkspaces(
            @AuthenticationPrincipal MemberDetails userDetails) {

        List<WorkspaceSummaryResponse> response = workspaceUseCase.getMyWorkspaces(userDetails.getMemberId());
        return ResponseEntity.ok(response);
    }
}
