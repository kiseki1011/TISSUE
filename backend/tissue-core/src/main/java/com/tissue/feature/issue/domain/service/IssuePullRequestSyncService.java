package com.tissue.feature.issue.domain.service;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssuePullRequest;
import com.tissue.feature.issue.domain.enums.PullRequestState;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Keeps an issue's linked pull requests current, mirroring {@link IssueBranchSyncService}.
 *
 * <p>Matches on the provider's PR number so a pull request that is opened, closed and reopened stays one
 * row that changes state, rather than three rows a reader has to reconcile.
 */
@Service
@Transactional
public class IssuePullRequestSyncService {

    /**
     * Creates or updates the issue's record of one pull request.
     *
     * @return the synced pull request, or null when the event carried no PR number to identify it by
     */
    @Nullable
    public IssuePullRequest syncPullRequest(Issue issue, GitPrDto gitPr) {
        if (gitPr.number() == null) {
            return null;
        }

        int number = gitPr.number();
        String title = gitPr.title() != null ? gitPr.title() : "";
        PullRequestState state = resolveState(gitPr);

        IssuePullRequest pullRequest = issue.getPullRequests().stream()
                .filter(pr -> pr.getNumber() == number)
                .findFirst()
                .orElse(null);

        if (pullRequest == null) {
            pullRequest = IssuePullRequest.create(
                    issue,
                    number,
                    title,
                    gitPr.htmlUrl() != null ? gitPr.htmlUrl() : "",
                    gitPr.authorUsername(),
                    state,
                    gitPr.occurredAt());
            issue.addPullRequest(pullRequest);
        } else {
            pullRequest.update(title, state, gitPr.authorUsername(), gitPr.occurredAt());
        }

        return pullRequest;
    }

    /**
     * Reads the provider's own flags rather than the action, so an event Tissue takes no other action on
     * still leaves the state right. A merged PR arrives as closed, so merged is checked first.
     */
    private PullRequestState resolveState(GitPrDto gitPr) {
        if (gitPr.merged()) {
            return PullRequestState.MERGED;
        }
        if (gitPr.closed()) {
            return PullRequestState.CLOSED;
        }
        return PullRequestState.OPEN;
    }
}
