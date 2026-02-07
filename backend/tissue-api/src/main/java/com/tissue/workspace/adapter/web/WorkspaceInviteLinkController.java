package com.tissue.workspace.adapter.web;

import com.tissue.global.security.principal.MemberDetails;
import com.tissue.workspace.adapter.web.request.CreateWorkspaceInviteLinkRequest;
import com.tissue.workspace.adapter.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.response.command.InviteLinkResponse;
import com.tissue.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.workspace.application.port.in.WorkspaceLinkUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceInviteLinkController {

    private final WorkspaceLinkUseCase linkUseCase;

    @PostMapping("/inviteLinks")
    public ResponseEntity<InviteLinkResponse> createWorkspaceLink(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateWorkspaceInviteLinkRequest request,
            @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        var command = request.toCommand();
        String token = linkUseCase.createWorkspaceLink(command, currentWorkspaceMember);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/workspaces/{workspaceKey}/inviteLinks/{token}")
                .buildAndExpand(workspaceKey, token)
                .toUri();

        return ResponseEntity.created(location)
                .body(new InviteLinkResponse(token, location.toString(), request.expiredAt()));
    }

    @DeleteMapping("/inviteLinks/{token}")
    public ResponseEntity<Void> expireLink(
            @PathVariable String token, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        linkUseCase.expireLink(token, currentWorkspaceMember);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inviteLinks/{token}/join")
    public ResponseEntity<WorkspaceMemberResponse> joinViaLink(
            @PathVariable String workspaceKey,
            @PathVariable String token,
            @AuthenticationPrincipal MemberDetails currentMember) {

        WorkspaceMemberResponse response = linkUseCase.joinViaLink(workspaceKey, token, currentMember.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/inviteLinks/{token}")
    public ResponseEntity<WorkspaceInviteLinkDetail> getLinkInfo(
            @PathVariable String token, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        WorkspaceInviteLinkDetail response = linkUseCase.getLinkDetail(token, currentWorkspaceMember);
        return ResponseEntity.ok(response);
    }
}
