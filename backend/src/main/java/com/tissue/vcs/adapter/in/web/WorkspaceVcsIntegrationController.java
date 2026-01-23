package com.tissue.vcs.adapter.in.web;

import com.tissue.project.adapter.in.web.resolver.CurrentProjectMember;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.vcs.adapter.in.web.dto.response.VcsIntegrationDetail;
import com.tissue.vcs.adapter.in.web.dto.response.VcsSecretResponse;
import com.tissue.vcs.application.port.in.WorkspaceVcsCommandUseCase;
import com.tissue.vcs.application.port.in.WorkspaceVcsQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/integrations/github")
@RequiredArgsConstructor
public class WorkspaceVcsIntegrationController {

    private final WorkspaceVcsCommandUseCase commandUseCase;
    private final WorkspaceVcsQueryUseCase queryUseCase;

    @GetMapping
    public ResponseEntity<VcsIntegrationDetail> getIntegration(
            @PathVariable String workspaceKey, @CurrentProjectMember ProjectMemberContext actorContext) {

        return ResponseEntity.ok(queryUseCase.getIntegration(workspaceKey, actorContext));
    }

    @PostMapping("/secret")
    public ResponseEntity<VcsSecretResponse> regenerateSecret(
            @PathVariable String workspaceKey, @CurrentProjectMember ProjectMemberContext actorContext) {

        return ResponseEntity.ok(commandUseCase.regenerateSecret(workspaceKey, actorContext));
    }

    @DeleteMapping
    public ResponseEntity<Void> removeIntegration(
            @PathVariable String workspaceKey, @CurrentProjectMember ProjectMemberContext actorContext) {

        commandUseCase.removeIntegration(workspaceKey, actorContext);
        return ResponseEntity.noContent().build();
    }
}
