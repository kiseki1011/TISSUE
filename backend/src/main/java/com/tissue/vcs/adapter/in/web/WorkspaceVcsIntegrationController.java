package com.tissue.vcs.adapter.in.web;

import com.tissue.vcs.adapter.in.web.dto.response.VcsIntegrationDetail;
import com.tissue.vcs.adapter.in.web.dto.response.VcsSecretResponse;
import com.tissue.vcs.application.port.in.WorkspaceVcsCommandUseCase;
import com.tissue.vcs.application.port.in.WorkspaceVcsQueryUseCase;
import com.tissue.vcs.domain.enums.VcsProvider;
import com.tissue.workspace.adapter.in.web.resolver.CurrentWorkspaceMember;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/integrations")
@RequiredArgsConstructor
public class WorkspaceVcsIntegrationController {

    private final WorkspaceVcsCommandUseCase commandUseCase;
    private final WorkspaceVcsQueryUseCase queryUseCase;

    @GetMapping("/github")
    public ResponseEntity<VcsIntegrationDetail> getGithubIntegration(
            @PathVariable String workspaceKey, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        return ResponseEntity.ok(queryUseCase.getIntegration(workspaceKey, VcsProvider.GITHUB, currentWorkspaceMember));
    }

    @PostMapping("/github/secret")
    public ResponseEntity<VcsSecretResponse> regenerateGithubSecret(
            @PathVariable String workspaceKey, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        return ResponseEntity.ok(
                commandUseCase.regenerateSecret(workspaceKey, VcsProvider.GITHUB, currentWorkspaceMember));
    }

    @DeleteMapping("/github")
    public ResponseEntity<Void> removeGithubIntegration(
            @PathVariable String workspaceKey, @CurrentWorkspaceMember WorkspaceMemberContext currentWorkspaceMember) {

        commandUseCase.removeIntegration(workspaceKey, VcsProvider.GITHUB, currentWorkspaceMember);
        return ResponseEntity.noContent().build();
    }
}
