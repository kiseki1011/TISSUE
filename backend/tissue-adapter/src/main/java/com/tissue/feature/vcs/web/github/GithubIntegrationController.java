package com.tissue.feature.vcs.web.github;

import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.usecase.WorkspaceVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.usecase.WorkspaceVcsQueryUseCase;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.principal.CurrentMember;
import com.tissue.principal.MemberDetails;
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
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        VcsIntegrationDetail response =
                queryUseCase.getIntegration(workspaceKey, VcsProvider.GITHUB, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/github/secret")
    public ResponseEntity<VcsSecretResponse> regenerateGithubSecret(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        VcsSecretResponse response =
                commandUseCase.regenerateSecret(workspaceKey, VcsProvider.GITHUB, memberDetails.getMemberId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/github")
    public ResponseEntity<Void> removeGithubIntegration(
            @PathVariable String workspaceKey, @CurrentMember MemberDetails memberDetails) {

        commandUseCase.removeIntegration(workspaceKey, VcsProvider.GITHUB, memberDetails.getMemberId());
        return ResponseEntity.noContent().build();
    }
}
