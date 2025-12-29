package com.tissue.workspace.adapter.in.web;

import com.tissue.security.authentication.MemberUserDetails;
import com.tissue.security.authentication.resolver.CurrentMember;
import com.tissue.workspace.adapter.in.web.dto.request.CreateProjectInviteLinkRequest;
import com.tissue.workspace.adapter.in.web.dto.request.CreateWorkspaceInviteLinkRequest;
import com.tissue.workspace.application.dto.in.ExpireLinkCommand;
import com.tissue.workspace.application.dto.in.JoinViaLinkCommand;
import com.tissue.workspace.application.dto.out.command.InviteLinkResponse;
import com.tissue.workspace.application.dto.out.command.WorkspaceMemberResponse;
import com.tissue.workspace.application.dto.out.query.WorkspaceInviteLinkDetail;
import com.tissue.workspace.application.port.in.WorkspaceInviteLinkUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}/inviteLinks")
public class WorkspaceInviteLinkController {

    private final WorkspaceInviteLinkUseCase inviteLinkUseCase;

    @PostMapping
    public ResponseEntity<InviteLinkResponse> createWorkspaceLink(
            @PathVariable String workspaceKey, @RequestBody @Valid CreateWorkspaceInviteLinkRequest request) {
        var command = request.toCommand(workspaceKey);
        String token = inviteLinkUseCase.createWorkspaceLink(command);

        // TODO: do i have to write the full uri path? cant i just do "/{token}"?
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/workspaces/{workspaceKey}/inviteLinks/{token}")
                .buildAndExpand(workspaceKey, token)
                .toUri();

        return ResponseEntity.created(location)
                .body(new InviteLinkResponse(token, location.toString(), request.expiredAt()));
    }

    @PostMapping("/projects/{projectKey}")
    public ResponseEntity<InviteLinkResponse> createProjectLink(
            @PathVariable String workspaceKey,
            @PathVariable String projectKey,
            @RequestBody @Valid CreateProjectInviteLinkRequest request) {
        var command = request.toCommand(workspaceKey, projectKey);
        String token = inviteLinkUseCase.createProjectLink(command);

        // TODO: do i have to write the full uri path? cant i just do "/{token}"?
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/workspaces/{workspaceKey}/inviteLinks/{token}")
                .buildAndExpand(workspaceKey, token)
                .toUri();

        return ResponseEntity.created(location)
                .body(new InviteLinkResponse(token, location.toString(), request.expiredAt()));
    }

    @DeleteMapping("/{token}")
    public ResponseEntity<Void> expireLink(@PathVariable String workspaceKey, @PathVariable String token) {
        var command = new ExpireLinkCommand(workspaceKey, token);
        inviteLinkUseCase.expireLink(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{token}/join")
    public ResponseEntity<WorkspaceMemberResponse> joinViaLink(
            @PathVariable String workspaceKey,
            @PathVariable String token,
            @CurrentMember MemberUserDetails userDetails) {
        var command = new JoinViaLinkCommand(workspaceKey, token, userDetails.getMemberId());
        WorkspaceMemberResponse response = inviteLinkUseCase.joinViaLink(command);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{token}")
    public ResponseEntity<WorkspaceInviteLinkDetail> getLinkInfo(
            @PathVariable String workspaceKey, @PathVariable String token) {
        WorkspaceInviteLinkDetail response = inviteLinkUseCase.getLinkInfo(workspaceKey, token);
        return ResponseEntity.ok(response);
    }
}
