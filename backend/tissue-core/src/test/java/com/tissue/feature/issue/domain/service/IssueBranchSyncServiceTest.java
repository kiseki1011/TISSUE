package com.tissue.feature.issue.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssueBranch;
import com.tissue.feature.vcs.application.dto.GitPushDto;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssueBranchSyncServiceTest {

    private final IssueBranchSyncService sut = new IssueBranchSyncService();

    @Test
    @DisplayName("success: a branch seen for the first time reports itself as newly linked")
    void firstPushLinksBranch() {
        // given
        Issue issue = issueWithBranches(new HashSet<>());

        // when
        IssueBranchSyncService.BranchSync sync = sut.syncBranch(issue, pushDto("abc1234", "add login"));

        // then
        assertThat(sync.newlyLinked()).isTrue();
        assertThat(sync.branch().getBranchName()).isEqualTo("feature/PROJ-12");
    }

    @Test
    @DisplayName("success: a later push moves the same branch instead of linking a second one")
    void laterPushUpdatesSameBranch() {
        // given
        Set<IssueBranch> branches = new HashSet<>();
        Issue issue = issueWithBranches(branches);
        branches.add(sut.syncBranch(issue, pushDto("abc1234", "add login")).branch());

        // when
        IssueBranchSyncService.BranchSync sync = sut.syncBranch(issue, pushDto("def5678", "fix redirect"));

        // then
        assertThat(sync.newlyLinked()).isFalse();
        assertThat(branches).hasSize(1);
        assertThat(sync.branch().getLatestCommitHash()).isEqualTo("def5678");
        assertThat(sync.branch().getLatestCommitMessage()).isEqualTo("fix redirect");
    }

    private Issue issueWithBranches(Set<IssueBranch> branches) {
        Issue issue = mock(Issue.class);
        given(issue.getBranches()).willReturn(branches);
        return issue;
    }

    private GitPushDto pushDto(String commitHash, String commitMessage) {
        return GitPushDto.builder()
                .projectKey("PROJ")
                .provider(VcsProvider.GITHUB)
                .ref("refs/heads/feature/PROJ-12")
                .repoUrl("https://github.com/acme/repo")
                .pusherName("octocat")
                .pusherEmail("octocat@example.com")
                .latestCommitHash(commitHash)
                .latestCommitMessage(commitMessage)
                .latestCommitUrl("https://github.com/acme/repo/commit/" + commitHash)
                .occurredAt(Instant.now())
                .build();
    }
}
