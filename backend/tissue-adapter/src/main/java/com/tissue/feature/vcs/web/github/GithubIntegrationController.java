package com.tissue.feature.vcs.web.github;

import com.tissue.feature.project.domain.exception.ProjectErrorCode;
import com.tissue.feature.vcs.application.dto.response.VcsIntegrationDetail;
import com.tissue.feature.vcs.application.dto.response.VcsSecretResponse;
import com.tissue.feature.vcs.application.port.usecase.ProjectVcsCommandUseCase;
import com.tissue.feature.vcs.application.port.usecase.ProjectVcsQueryUseCase;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.feature.vcs.domain.exception.VcsErrorCode;
import com.tissue.global.openapi.ProjectErrors;
import com.tissue.global.openapi.VcsErrors;
import com.tissue.security.principal.CurrentMember;
import com.tissue.security.principal.MemberDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "GitHub Integration")
@RestController
@RequestMapping("/api/v1/projects/{projectKey}/integrations")
@RequiredArgsConstructor
public class GithubIntegrationController {

    private final ProjectVcsCommandUseCase commandUseCase;
    private final ProjectVcsQueryUseCase queryUseCase;

    @Operation(operationId = "getGithubIntegration", summary = "Get GitHub integration", description = """
                Retrieve the GitHub integration details for a project.

                **Requirements:**
                - Requires project membership""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Integration details retrieved"),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND})
    @VcsErrors({VcsErrorCode.INTEGRATION_NOT_FOUND})
    @GetMapping("/github")
    public ResponseEntity<VcsIntegrationDetail> getGithubIntegration(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        VcsIntegrationDetail response =
                queryUseCase.getIntegration(projectKey, VcsProvider.GITHUB, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "regenerateGithubSecret", summary = "Regenerate GitHub webhook secret", description = """
                Regenerate the webhook secret used to verify GitHub webhook payloads.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "New secret generated"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @PostMapping("/github:regenerateSecret")
    public ResponseEntity<VcsSecretResponse> regenerateGithubSecret(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        VcsSecretResponse response =
                commandUseCase.regenerateSecret(projectKey, VcsProvider.GITHUB, memberDetails.getMemberId());

        return ResponseEntity.ok(response);
    }

    @Operation(operationId = "removeGithubIntegration", summary = "Remove GitHub integration", description = """
                Remove the GitHub integration from a project. This will also invalidate the webhook secret.

                **Requirements:**
                - Requires project `MANAGER` or higher role""")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Integration removed"),
        @ApiResponse(responseCode = "403", description = "Insufficient permission", content = @Content),
        @ApiResponse(responseCode = "404", description = "Resource not found", content = @Content)
    })
    @ProjectErrors({
        ProjectErrorCode.PROJECT_MEMBER_NOT_FOUND,
        ProjectErrorCode.PROJECT_MANAGER_REQUIRED,
    })
    @VcsErrors({VcsErrorCode.INTEGRATION_NOT_FOUND})
    @DeleteMapping("/github")
    public ResponseEntity<Void> removeGithubIntegration(
            @PathVariable String projectKey, @CurrentMember MemberDetails memberDetails) {
        commandUseCase.removeIntegration(projectKey, VcsProvider.GITHUB, memberDetails.getMemberId());

        return ResponseEntity.noContent().build();
    }
}
