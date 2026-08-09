package com.tissue.feature.issue.application.dto.response;

import com.tissue.feature.issue.domain.IssueBranch;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * A VCS branch linked to an issue, surfaced read-only on the issue detail. Populated from inbound push
 * webhooks: the branch and latest-commit URLs are ready-to-open links, so the client can deep-link to the
 * repository without composing URLs itself. Commit fields are nullable until a push has been seen.
 */
@Schema(description = "A VCS branch linked to the issue, with ready-to-open repository links.")
public record IssueBranchView(
        @Schema(description = "Repository URL the branch lives in")
        String repoUrl,

        @Schema(description = "Branch name, e.g. feature/PROJ-12-login")
        String branchName,

        @Schema(description = "Ready-to-open URL of the branch")
        String branchUrl,

        @Schema(description = "Short hash of the latest pushed commit") @Nullable
        String latestCommitHash,

        @Schema(description = "Message of the latest pushed commit") @Nullable
        String latestCommitMessage,

        @Schema(description = "Ready-to-open URL of the latest commit") @Nullable
        String latestCommitUrl,

        @Schema(description = "Display name of who pushed last") @Nullable
        String pusherName,

        @Schema(description = "When the latest commit was pushed") @Nullable
        Instant pushedAt) {

    public static IssueBranchView from(IssueBranch branch) {
        return new IssueBranchView(
                branch.getRepoUrl(),
                branch.getBranchName(),
                branch.getBranchUrl(),
                branch.getLatestCommitHash(),
                branch.getLatestCommitMessage(),
                branch.getLatestCommitUrl(),
                branch.getPusherName(),
                branch.getPushedAt());
    }
}
