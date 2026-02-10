package com.tissue.vcs.web.github;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.in.WorkspaceVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.in.WorkspaceVcsQueryUseCase;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.web.resolver.CurrentWorkspaceMember;
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
public class GithubIntegrationController {

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
