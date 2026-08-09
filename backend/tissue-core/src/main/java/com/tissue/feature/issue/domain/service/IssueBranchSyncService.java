package com.tissue.feature.issue.domain.service;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class IssueBranchSyncService {

    private static final String REFS_HEADS_PREFIX = "refs/heads/";

    /**
     * The outcome of a sync. {@code newlyLinked} separates the branch first appearing on the issue - which
     * happens once and is worth recording - from a later push to it, which only moves the commit the branch
     * already shows.
     */
    public record BranchSync(IssueBranch branch, boolean newlyLinked) {}

    /**
     * Synchronizes a branch with an issue.
     * If a branch with the same name already exists, it updates the latest commit info.
     * Otherwise, it creates a new branch and associates it with the issue.
     *
     * @param issue   The issue to sync with.
     * @param gitPush The push event data.
     * @return The synced branch, and whether this push is what linked it.
     */
    public BranchSync syncBranch(Issue issue, GitPushDto gitPush) {
        String branchName = gitPush.ref().replace(REFS_HEADS_PREFIX, "");
        String branchUrl = gitPush.provider().buildBranchUrl(gitPush.repoUrl(), branchName);

        IssueBranch branch = issue.getBranches().stream()
                .filter(b -> b.getBranchName().equals(branchName))
                .findFirst()
                .orElse(null);

        if (branch == null) {
            branch = IssueBranch.create(
                    issue,
                    gitPush.repoUrl(),
                    branchName,
                    branchUrl,
                    gitPush.latestCommitHash(),
                    gitPush.latestCommitMessage(),
                    gitPush.latestCommitUrl(),
                    gitPush.pusherName(),
                    gitPush.occurredAt());
            issue.addBranch(branch);
            return new BranchSync(branch, true);
        }

        branch.updateLatestCommit(
                gitPush.latestCommitHash(),
                gitPush.latestCommitMessage(),
                gitPush.latestCommitUrl(),
                gitPush.pusherName(),
                gitPush.occurredAt());

        return new BranchSync(branch, false);
    }
}
