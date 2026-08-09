package com.tissue.feature.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.tissue.TestcontainersConfiguration;
import com.tissue.feature.issue.domain.event.IssueAssignedEvent;
import com.tissue.feature.issue.domain.event.IssueTransitionedBySystemEvent;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.realtime.application.SseEmitterRegistry;
import com.tissue.feature.vcs.domain.enums.VcsProvider;
import com.tissue.security.domain.TokenProvider;
import com.tissue.support.DatabaseCleanup;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RealtimeStreamIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    private TokenProvider tokenProvider;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SseEmitterRegistry sseEmitterRegistry;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @BeforeEach
    void setUp() {
        databaseCleanup.execute();
    }

    @Test
    @DisplayName("success: a subscribed project member receives an issue event over the SSE stream")
    void memberReceivesPublishedIssueEvent() throws Exception {
        // given - a member of a project, subscribed to the realtime stream
        Member member = memberCommandRepository.save(Member.create("watcher@tissue.dev", "watcher", "Watcher"));
        Project project = projectCommandRepository.save(Project.create("RTIME", "Realtime", "Realtime test"));
        projectMemberCommandRepository.save(ProjectMember.create(project, member));

        String accessToken = tokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority())));

        HttpResponse<Stream<String>> response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/events/stream"))
                                .header("Authorization", "Bearer " + accessToken)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofLines());

        assertThat(response.statusCode()).isEqualTo(200);

        // the stream has no replay buffer, so wait until the server registered this subscriber
        await().atMost(Duration.ofSeconds(5)).until(() -> sseEmitterRegistry.hasMember(member.getId()));

        // when - an issue event for that project is published and committed
        String issueKey = project.getKey() + "-1";
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publishEvent(IssueAssignedEvent.create(
                project.getKey(),
                issueKey,
                member.getId(),
                member.getUsername(),
                member.getId(),
                member.getUsername())));

        // then - the matching SSE data frame arrives on the stream
        try (Stream<String> lines = response.body()) {
            Optional<String> frame = CompletableFuture.supplyAsync(
                            () -> lines.filter(line -> line.startsWith("data:") && line.contains(issueKey))
                                    .findFirst())
                    .get(5, TimeUnit.SECONDS);

            assertThat(frame).isPresent();
            assertThat(frame.get()).contains("ISSUE_ASSIGNED").contains(issueKey);
        }
    }

    @Test
    @DisplayName("success: a VCS-driven transition with no member to attribute it to still reaches the stream")
    void memberReceivesSystemTransitionEvent() throws Exception {
        // given - a member of a project, subscribed to the realtime stream
        Member member = memberCommandRepository.save(Member.create("watcher@tissue.dev", "watcher", "Watcher"));
        Project project = projectCommandRepository.save(Project.create("RTIME", "Realtime", "Realtime test"));
        projectMemberCommandRepository.save(ProjectMember.create(project, member));

        String accessToken = tokenProvider.createAccessToken(
                member.getId(),
                member.getEmail(),
                member.getUsername(),
                List.of(new SimpleGrantedAuthority(member.getRole().getAuthority())));

        HttpResponse<Stream<String>> response = HttpClient.newHttpClient()
                .send(
                        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/events/stream"))
                                .header("Authorization", "Bearer " + accessToken)
                                .GET()
                                .build(),
                        HttpResponse.BodyHandlers.ofLines());

        assertThat(response.statusCode()).isEqualTo(200);
        await().atMost(Duration.ofSeconds(5)).until(() -> sseEmitterRegistry.hasMember(member.getId()));

        // when - a pull request merge moves the issue with no matching project member, so the event carries
        // no actor at all: the case a non-nullable actor would have silently dropped
        String issueKey = project.getKey() + "-1";
        transactionTemplate.executeWithoutResult(
                status -> eventPublisher.publishEvent(IssueTransitionedBySystemEvent.create(
                        project.getKey(),
                        issueKey,
                        null,
                        1L,
                        "Complete",
                        1L,
                        "In Progress",
                        2L,
                        "Done",
                        VcsProvider.GITHUB,
                        "octocat@example.com",
                        "octocat",
                        "GITHUB PR MERGED")));

        // then
        try (Stream<String> lines = response.body()) {
            Optional<String> frame = CompletableFuture.supplyAsync(
                            () -> lines.filter(line -> line.startsWith("data:") && line.contains(issueKey))
                                    .findFirst())
                    .get(5, TimeUnit.SECONDS);

            assertThat(frame).isPresent();
            assertThat(frame.get()).contains("ISSUE_TRANSITIONED_BY_SYSTEM").contains("Done");
        }
    }
}
