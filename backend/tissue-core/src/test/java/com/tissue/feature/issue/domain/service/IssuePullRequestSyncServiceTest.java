package com.tissue.feature.issue.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.domain.Issue;
import com.tissue.feature.issue.domain.IssuePullRequest;
import com.tissue.feature.issue.domain.enums.PullRequestState;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class IssuePullRequestSyncServiceTest {

    private final IssuePullRequestSyncService sut = new IssuePullRequestSyncService();

    @Test
    @DisplayName("success: a new pull request is linked as open")
    void linksNewPullRequestAsOpen() {
        // given
        Issue issue = issueWithPullRequests(new HashSet<>());

        // when
        IssuePullRequest pullRequest = sut.syncPullRequest(issue, prDto(PrAction.OPENED, 42, false, false));

        // then
        assertThat(pullRequest).isNotNull();
        assertThat(pullRequest.getNumber()).isEqualTo(42);
        assertThat(pullRequest.getState()).isEqualTo(PullRequestState.OPEN);
    }

    @Test
    @DisplayName("success: the same PR number updates in place rather than piling up rows")
    void updatesExistingPullRequest() {
        // given
        Set<IssuePullRequest> existing = new HashSet<>();
        Issue issue = issueWithPullRequests(existing);
        IssuePullRequest opened = sut.syncPullRequest(issue, prDto(PrAction.OPENED, 42, false, false));
        existing.add(opened);

        // when
        IssuePullRequest merged = sut.syncPullRequest(issue, prDto(PrAction.MERGED, 42, true, true));

        // then
        assertThat(merged).isSameAs(opened);
        assertThat(merged.getState()).isEqualTo(PullRequestState.MERGED);
    }

    @Test
    @DisplayName("success: a closed but unmerged PR reads as closed")
    void closedWithoutMergeIsClosed() {
        // given
        Issue issue = issueWithPullRequests(new HashSet<>());

        // when
        IssuePullRequest pullRequest = sut.syncPullRequest(issue, prDto(PrAction.CLOSED, 42, false, true));

        // then
        assertThat(pullRequest).isNotNull();
        assertThat(pullRequest.getState()).isEqualTo(PullRequestState.CLOSED);
    }

    @Test
    @DisplayName("success: an event Tissue does not act on still refreshes the state")
    void unhandledActionStillRefreshesState() {
        // given
        Issue issue = issueWithPullRequests(new HashSet<>());

        // when: GitHub sends this for a new commit pushed to an open PR
        IssuePullRequest pullRequest = sut.syncPullRequest(issue, prDto(PrAction.UNKNOWN, 42, false, false));

        // then
        assertThat(pullRequest).isNotNull();
        assertThat(pullRequest.getState()).isEqualTo(PullRequestState.OPEN);
    }

    @Test
    @DisplayName("ignore: an event with no PR number cannot identify a pull request")
    void ignoresEventWithoutNumber() {
        // given
        Issue issue = issueWithPullRequests(new HashSet<>());

        // when
        IssuePullRequest pullRequest = sut.syncPullRequest(issue, prDto(PrAction.OPENED, null, false, false));

        // then
        assertThat(pullRequest).isNull();
    }

    private Issue issueWithPullRequests(Set<IssuePullRequest> pullRequests) {
        Issue issue = mock(Issue.class);
        given(issue.getPullRequests()).willReturn(pullRequests);
        return issue;
    }

    private GitPrDto prDto(PrAction action, Integer number, boolean merged, boolean closed) {
        return GitPrDto.builder()
                .projectKey("PROJ")
                .provider(VcsProvider.GITHUB)
                .action(action)
                .number(number)
                .title("PROJ-12: add login")
                .htmlUrl("https://github.com/acme/repo/pull/42")
                .authorUsername("octocat")
                .authorEmail("octocat@example.com")
                .occurredAt(Instant.now())
                .merged(merged)
                .closed(closed)
                .build();
    }
}
