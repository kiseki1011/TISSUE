package com.tissue.feature.workspace.web;

import com.tissue.feature.workspace.application.dto.response.command.InviteLinkResponse;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceMemberResponse;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkUseCase;
import com.tissue.feature.workspace.web.request.CreateWorkspaceInviteLinkRequest;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
@RequestMapping("/api/v1/workspaces/{workspaceKey}")
public class WorkspaceInviteLinkController {

    private final WorkspaceLinkUseCase linkUseCase;

    @PostMapping("/inviteLinks")
    public ResponseEntity<InviteLinkResponse> createWorkspaceLink(
            @PathVariable String workspaceKey,
            @RequestBody @Valid CreateWorkspaceInviteLinkRequest request,
            @CurrentMember MemberDetails memberDetails) {

        var command = request.toCommand();
        String token = linkUseCase.createWorkspaceLink(workspaceKey, command, memberDetails.getMemberId());

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/workspaces/{workspaceKey}/inviteLinks/{token}")
                .buildAndExpand(workspaceKey, token)
                .toUri();

        return ResponseEntity.created(location).body(new InviteLinkResponse(token, request.expiredAt()));
    }

    @DeleteMapping("/inviteLinks/{token}")
    public ResponseEntity<Void> deleteLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {

        linkUseCase.deleteLink(workspaceKey, token, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/inviteLinks/{token}/join")
    public ResponseEntity<WorkspaceMemberResponse> joinViaLink(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {

        WorkspaceMemberResponse response = linkUseCase.joinViaLink(workspaceKey, token, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inviteLinks")
    public ResponseEntity<List<WorkspaceInviteLinkDetail>> getWorkspaceLinks(
        @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        List<WorkspaceInviteLinkDetail> response =
            linkUseCase.getWorkspaceLinks(workspaceKey, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/inviteLinks/{token}")
    public ResponseEntity<WorkspaceInviteLinkDetail> getLinkInfo(
            @PathVariable String workspaceKey, @PathVariable String token, @CurrentMember MemberDetails memberDetails) {

        WorkspaceInviteLinkDetail response =
                linkUseCase.getLinkDetail(workspaceKey, token, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }
}
