package com.tissue.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.TestcontainersConfiguration;
import com.tissue.feature.issue.domain.enums.IssueHierarchy;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.issuetype.domain.IssueField;
import com.tissue.feature.issuetype.domain.IssueType;
import com.tissue.feature.issuetype.domain.enums.IssueFieldType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.security.application.dto.GeneratedToken;
import com.tissue.security.application.service.PersonalAccessTokenService;
import com.tissue.security.domain.PatScope;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.enums.IconType;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import com.tissue.shared.vo.Name;
import com.tissue.support.DatabaseCleanup;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Content;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.NOT_REVIEWED,
        model = "claude-opus-4-8")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class McpWriteToolRoundTripIntegrationTest {

    private static final Pattern ISSUE_KEY = Pattern.compile("\"issueKey\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern COMMENT_ID = Pattern.compile("\"commentId\"\\s*:\\s*(\\d+)");
    private static final String TEAMMATE_USERNAME = "teammate-bot";

    @LocalServerPort
    private int port;

    @Autowired
    private MemberCommandRepository memberRepository;

    @Autowired
    private ProjectCommandRepository projectRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private IssueTypeRepository issueTypeRepository;

    @Autowired
    private PersonalAccessTokenService personalAccessTokenService;

    @Autowired
    private DatabaseCleanup databaseCleanup;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private Fixture fixture;

    @BeforeEach
    void setUp() {
        databaseCleanup.execute();
        fixture = new TransactionTemplate(transactionManager).execute(status -> {
            Member agent = memberRepository.save(Member.create("agent@tissue.dev", "agent-007", "Agent 007"));
            Member teammate =
                    memberRepository.save(Member.create("teammate@tissue.dev", TEAMMATE_USERNAME, "Teammate"));

            Project project = projectRepository.save(Project.create("PROJ", "Proj", null));
            projectMemberRepository.save(ProjectMember.create(project, agent));
            projectMemberRepository.save(ProjectMember.create(project, teammate));

            Workflow workflow = Workflow.create(Name.of("Default"), null, ColorType.YELLOW);
            WorkflowState todo = workflow.addState(Name.of("TODO"), null, ColorType.GREEN, StateCategory.INITIAL);
            WorkflowState inProgress =
                    workflow.addState(Name.of("IN PROGRESS"), null, ColorType.BLUE, StateCategory.ACTIVE);
            workflow.addTransition(Name.of("Start"), null, todo, inProgress);
            workflowRepository.save(workflow);

            IssueType issueType = IssueType.create(
                    Name.of("Story"), null, ColorType.RED, IconType.CIRCLE_FILLED, IssueHierarchy.STANDARD, workflow);
            IssueField note = issueType.addField(Name.of("note"), "free text", IssueFieldType.TEXT, false, 0);
            issueTypeRepository.save(issueType);

            GeneratedToken writeToken = personalAccessTokenService.generate(agent, "rw", PatScope.READ_WRITE, null);
            GeneratedToken readToken = personalAccessTokenService.generate(agent, "ro", PatScope.READ_ONLY, null);

            Long transitionId =
                    workflow.getTransitions().stream().findFirst().orElseThrow().getId();

            return new Fixture(
                    issueType.getId(),
                    note.getId(),
                    transitionId,
                    teammate.getId(),
                    writeToken.rawToken(),
                    readToken.rawToken());
        });
    }

    @Test
    @DisplayName("success: a READ_WRITE agent can create, claim, then transition an issue over MCP")
    void writeChainOverMcp() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            CallToolResult created = client.callTool(new CallToolRequest(
                    "create_issue",
                    Map.of(
                            "projectKey", "PROJ",
                            "issueTypeId", fixture.issueTypeId(),
                            "title", "Agent task",
                            "priority", "P2")));
            assertThat(created.isError()).isNotEqualTo(true);
            String issueKey = extractIssueKey(textOf(created));
            assertThat(issueKey).startsWith("PROJ-");

            CallToolResult claimed = client.callTool(new CallToolRequest("claim_issue", Map.of("issueKey", issueKey)));
            assertThat(claimed.isError()).isNotEqualTo(true);
            assertThat(textOf(claimed)).contains(issueKey);

            CallToolResult transitioned = client.callTool(new CallToolRequest(
                    "transition_issue", Map.of("issueKey", issueKey, "transitionId", fixture.transitionId())));
            assertThat(transitioned.isError()).isNotEqualTo(true);
            assertThat(textOf(transitioned)).contains("IN PROGRESS");
        }
    }

    @Test
    @DisplayName("success: create_issue binds dueAt, storyPoint and the customFields map end-to-end")
    void createWithCustomFieldsAndDueAt() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            Map<String, Object> args = new HashMap<>();
            args.put("projectKey", "PROJ");
            args.put("issueTypeId", fixture.issueTypeId());
            args.put("title", "Rich task");
            args.put("priority", "P1");
            args.put("content", "do the thing");
            args.put("dueAt", "2026-12-31T23:59:59Z");
            args.put("storyPoint", 5);
            args.put("customFields", Map.of(String.valueOf(fixture.noteFieldId()), "remember-me"));

            CallToolResult created = client.callTool(new CallToolRequest("create_issue", args));
            assertThat(created.isError()).isNotEqualTo(true);
            String issueKey = extractIssueKey(textOf(created));

            // read it back to confirm the custom field value persisted through the conversion
            CallToolResult fetched = client.callTool(new CallToolRequest("get_issue", Map.of("issueKey", issueKey)));
            assertThat(fetched.isError()).isNotEqualTo(true);
            assertThat(textOf(fetched)).contains("remember-me");
        }
    }

    @Test
    @DisplayName("success: update_issue changes only the provided fields over MCP")
    void updateOverMcp() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            String issueKey = extractIssueKey(textOf(client.callTool(new CallToolRequest(
                    "create_issue",
                    Map.of(
                            "projectKey", "PROJ",
                            "issueTypeId", fixture.issueTypeId(),
                            "title", "Original title",
                            "priority", "P3")))));

            // update title + priority + a custom field - content is omitted, so it stays unchanged
            Map<String, Object> args = new HashMap<>();
            args.put("issueKey", issueKey);
            args.put("title", "Updated title");
            args.put("priority", "P0");
            args.put("customFields", Map.of(String.valueOf(fixture.noteFieldId()), "added-note"));

            CallToolResult updated = client.callTool(new CallToolRequest("update_issue", args));

            assertThat(updated.isError()).isNotEqualTo(true);
            String payload = textOf(updated);
            assertThat(payload).contains("Updated title").contains("added-note").contains("P0");
            assertThat(payload).doesNotContain("Original title");
        }
    }

    @Test
    @DisplayName("success: assign_issue assigns to another member by id over MCP")
    void assignOverMcp() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            String issueKey = extractIssueKey(textOf(client.callTool(new CallToolRequest(
                    "create_issue",
                    Map.of(
                            "projectKey", "PROJ",
                            "issueTypeId", fixture.issueTypeId(),
                            "title", "To delegate",
                            "priority", "P3")))));

            CallToolResult assigned = client.callTool(new CallToolRequest(
                    "assign_issue", Map.of("issueKey", issueKey, "memberId", fixture.teammateMemberId())));

            assertThat(assigned.isError()).isNotEqualTo(true);
            assertThat(textOf(assigned)).contains(TEAMMATE_USERNAME);
        }
    }

    @Test
    @DisplayName("success: add_comment posts a comment and a threaded reply over MCP")
    void addCommentOverMcp() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            String issueKey = extractIssueKey(textOf(client.callTool(new CallToolRequest(
                    "create_issue",
                    Map.of(
                            "projectKey", "PROJ",
                            "issueTypeId", fixture.issueTypeId(),
                            "title", "Discuss",
                            "priority", "P3")))));

            CallToolResult top = client.callTool(new CallToolRequest(
                    "add_comment",
                    Map.of(
                            "issueKey",
                            issueKey,
                            "content",
                            "starting work",
                            "mentionedUsernames",
                            List.of(TEAMMATE_USERNAME))));
            assertThat(top.isError()).isNotEqualTo(true);
            Long parentCommentId = extractCommentId(textOf(top));

            CallToolResult reply = client.callTool(new CallToolRequest(
                    "add_comment",
                    Map.of(
                            "issueKey", issueKey,
                            "content", "replying",
                            "parentCommentId", parentCommentId)));
            assertThat(reply.isError()).isNotEqualTo(true);
            assertThat(extractCommentId(textOf(reply))).isNotEqualTo(parentCommentId);
        }
    }

    @Test
    @DisplayName("fail: a READ_ONLY agent is rejected by the SCOPE_WRITE gate on create_issue")
    void readOnlyRejectedByWriteScopeGate() {
        try (McpSyncClient client = newClient(fixture.readToken())) {
            client.initialize();

            CallToolResult result = client.callTool(new CallToolRequest(
                    "create_issue",
                    Map.of(
                            "projectKey", "PROJ",
                            "issueTypeId", fixture.issueTypeId(),
                            "title", "Should fail",
                            "priority", "P2")));

            assertThat(result.isError()).isTrue();
            assertThat(textOf(result)).contains("SCOPE_WRITE");
        }
    }

    @Test
    @DisplayName("fail: a malformed dueAt comes back as an actionable tool error")
    void malformedDueAtRejected() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            Map<String, Object> args = new HashMap<>();
            args.put("projectKey", "PROJ");
            args.put("issueTypeId", fixture.issueTypeId());
            args.put("title", "Bad date");
            args.put("priority", "P2");
            args.put("dueAt", "2026-01-31");

            CallToolResult result = client.callTool(new CallToolRequest("create_issue", args));

            assertThat(result.isError()).isTrue();
            assertThat(textOf(result)).contains("ISO-8601");
        }
    }

    @Test
    @DisplayName("fail: a non-numeric customFields key comes back as an actionable tool error")
    void nonNumericCustomFieldKeyRejected() {
        try (McpSyncClient client = newClient(fixture.writeToken())) {
            client.initialize();

            Map<String, Object> args = new HashMap<>();
            args.put("projectKey", "PROJ");
            args.put("issueTypeId", fixture.issueTypeId());
            args.put("title", "Bad field key");
            args.put("priority", "P2");
            args.put("customFields", Map.of("note", "value"));

            CallToolResult result = client.callTool(new CallToolRequest("create_issue", args));

            assertThat(result.isError()).isTrue();
            assertThat(textOf(result)).contains("numeric field ids");
        }
    }

    private McpSyncClient newClient(String rawToken) {
        HttpClientStreamableHttpTransport transport = HttpClientStreamableHttpTransport.builder(
                        "http://localhost:" + port)
                .endpoint("/mcp/v1")
                .customizeRequest(builder -> builder.header("Authorization", "Bearer " + rawToken))
                .build();
        return McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(20))
                .initializationTimeout(Duration.ofSeconds(20))
                .build();
    }

    private static String extractIssueKey(String payload) {
        Matcher matcher = ISSUE_KEY.matcher(payload);
        assertThat(matcher.find())
                .as("payload should carry an issueKey: %s", payload)
                .isTrue();
        return matcher.group(1);
    }

    private static Long extractCommentId(String payload) {
        Matcher matcher = COMMENT_ID.matcher(payload);
        assertThat(matcher.find())
                .as("payload should carry a commentId: %s", payload)
                .isTrue();
        return Long.valueOf(matcher.group(1));
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

    private record Fixture(
            Long issueTypeId,
            Long noteFieldId,
            Long transitionId,
            Long teammateMemberId,
            String writeToken,
            String readToken) {}
}
