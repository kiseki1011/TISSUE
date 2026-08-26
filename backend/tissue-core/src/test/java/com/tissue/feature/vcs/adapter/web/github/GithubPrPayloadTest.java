package com.tissue.feature.vcs.adapter.web.github;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.feature.vcs.application.dto.GitPrDto;
import com.tissue.feature.vcs.domain.enums.PrAction;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GithubPrPayloadTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String PROJECT_KEY = "PROJ";

    @Test
    @DisplayName("success: a closed PR that was merged maps to MERGED")
    void mapsMergedPr() throws Exception {
        // given
        String payload = payloadWith("closed", true);

        // when
        GitPrDto dto = parse(payload);

        // then
        assertThat(dto.action()).isEqualTo(PrAction.MERGED);
        assertThat(dto.merged()).isTrue();
    }

    @Test
    @DisplayName("success: a closed PR that was not merged maps to CLOSED")
    void mapsClosedPr() throws Exception {
        // given
        String payload = payloadWith("closed", false);

        // when
        GitPrDto dto = parse(payload);

        // then
        assertThat(dto.action()).isEqualTo(PrAction.CLOSED);
        assertThat(dto.merged()).isFalse();
    }

    @Test
    @DisplayName("success: an opened PR maps to OPENED")
    void mapsOpenedPr() throws Exception {
        // given
        String payload = payloadWith("opened", false);

        // when
        GitPrDto dto = parse(payload);

        // then
        assertThat(dto.action()).isEqualTo(PrAction.OPENED);
    }

    @Test
    @DisplayName("success: an action Tissue does not act on maps to UNKNOWN")
    void mapsUnhandledActionToUnknown() throws Exception {
        // given
        String payload = payloadWith("labeled", false);

        // when
        GitPrDto dto = parse(payload);

        // then
        assertThat(dto.action()).isEqualTo(PrAction.UNKNOWN);
    }

    private GitPrDto parse(String payload) throws Exception {
        return objectMapper.readValue(payload, GithubPrPayload.class).toVcsDto(PROJECT_KEY, VcsProvider.GITHUB);
    }

    @Test
    @DisplayName("success: the PR number and provider state are carried through")
    void carriesNumberAndState() throws Exception {
        // given
        String payload = payloadWith("closed", true);

        // when
        GitPrDto dto = parse(payload);

        // then
        assertThat(dto.number()).isEqualTo(1);
        assertThat(dto.closed()).isTrue();
    }

    @Test
    @DisplayName("success: an open pull request is not reported as closed")
    void openPullRequestIsNotClosed() throws Exception {
        // given
        String payload = payloadWith("opened", false);

        // when
        GitPrDto dto = parse(payload);

        // then
        assertThat(dto.closed()).isFalse();
    }

    private String payloadWith(String action, boolean merged) {
        String state = merged || "closed".equals(action) ? "closed" : "open";
        return """
                {
                  "action": "%s",
                  "pull_request": {
                    "number": 1,
                    "state": "%s",
                    "title": "PROJ-1: add login",
                    "body": "body",
                    "html_url": "https://github.com/acme/repo/pull/1",
                    "merged": %s,
                    "user": { "login": "octocat", "email": "octocat@example.com" }
                  }
                }
                """.formatted(action, state, merged);
    }
}
