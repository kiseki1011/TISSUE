package com.tissue.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.TestcontainersConfiguration;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.security.application.service.PersonalAccessTokenService.GeneratedToken;
import com.tissue.security.domain.PatScope;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.support.DatabaseCleanup;
import com.tissue.support.RedisCleanup;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.InitializeResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Drives a real MCP streamable-HTTP round-trip against the embedded server.
 *
 * <p>Boots the full application on a real port, authenticates as a member through a Personal Access
 * Token, then connects an actual MCP client to {@code /mcp/v1} and performs the protocol handshake
 * ({@code initialize}), discovery ({@code tools/list}) and execution ({@code tools/call whoami}).
 */
@LLMGenerated(
    llmInvolvement = LLMInvolvement.ASSISTED,
    evaluation = Evaluation.ACCEPTABLE,
    reviewedBy = "kiseki1011"
)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class McpProtocolRoundTripIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private PersonalAccessTokenService personalAccessTokenService;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private RedisCleanup redisCleanup;

    @BeforeEach
    void setUp() {
        databaseCleanup.execute();
    }

    @Test
    @DisplayName("success: an agent connects with its PAT and whoami reports its own identity")
    void roundTripReportsAuthenticatedIdentity() {
        // given - a member holding a read-only PAT
        Member member = memberRepository.save(Member.create("agent@tissue.dev", "agent-007", "Agent 007"));
        GeneratedToken generated = personalAccessTokenService.generate(member, "ci-token", PatScope.READ_ONLY, null);
        String rawToken = generated.rawToken();

        try (McpSyncClient client = newClient(rawToken)) {
            // when
            InitializeResult initialization = client.initialize(); // protocol handshake

            // then
            assertThat(initialization).isNotNull();
            assertThat(initialization.serverInfo().name()).isEqualTo("tissue-mcp");

            // when
            ListToolsResult tools = client.listTools(); // tool discovery

            // then
            assertThat(tools.tools()).extracting(Tool::name).contains("whoami");

            // when
            CallToolResult result = client.callTool(new CallToolRequest("whoami", Map.of()));

            // then - call succeeds
            assertThat(result.isError()).isNotEqualTo(true);
            String payload = textOf(result);
            assertThat(payload)
                    .contains(String.valueOf(member.getId()))
                    .contains("agent-007")
                    .contains("SCOPE_READ")
                    .doesNotContain("SCOPE_WRITE");
        }
    }

    @Test
    @DisplayName("fail: a connection without a PAT cannot complete client initialization")
    void rejectsUnauthenticatedHandshake() {
        try (McpSyncClient client = newClient(null)) {
            assertThatThrownBy(client::initialize).isInstanceOf(RuntimeException.class);
        }
    }

    private McpSyncClient newClient(String rawToken) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(
                        "http://localhost:" + port)
                .endpoint("/mcp/v1")
                .customizeRequest(builder -> {
                    if (rawToken != null) {
                        builder.header("Authorization", "Bearer " + rawToken);
                    }
                })
                .build();
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .initializationTimeout(Duration.ofSeconds(20))
                .build();
    }

    private static String textOf(CallToolResult result) {
        StringBuilder text = new StringBuilder();
        for (Content content : result.content()) {
            if (content instanceof TextContent textContent) {
                text.append(textContent.text());
            }
        }
        return text.toString();
    }
}
