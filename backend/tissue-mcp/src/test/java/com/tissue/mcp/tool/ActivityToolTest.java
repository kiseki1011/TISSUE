package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.activitylog.application.dto.response.ActivityLogResponse;
import com.tissue.feature.activitylog.application.port.usecase.ActivityLogQueryUseCase;
import com.tissue.feature.activitylog.domain.ActivityType;
import com.tissue.shared.auth.MemberDetails;
import com.tissue.shared.dto.CursorPage;
import com.tissue.shared.dto.FieldChange;
import com.tissue.shared.dto.IssueIdentifier;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class ActivityToolTest {

    private final ActivityLogQueryUseCase activityLogQueryUseCase = mock(ActivityLogQueryUseCase.class);

    private final ActivityTool tool = new ActivityTool(activityLogQueryUseCase);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("success: the issue activity method is exposed as an MCP tool")
    void registersActivityToolAsMcpTool() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .containsExactly("get_issue_activity");
    }

    @Test
    @DisplayName("success: sprint activity is not offered, holding only a start and a completion")
    void exposesNoSprintActivityTool() {
        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(List.of(tool));

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .doesNotContain("get_sprint_activity");
    }

    @Test
    @DisplayName("success: each row comes back as a sentence, keeping the cursor for the next page")
    void rendersRowsAsSentences() {
        authenticate();
        given(activityLogQueryUseCase.getIssueActivities(any(), anyLong(), any(), anyInt()))
                .willReturn(new CursorPage<>(
                        List.of(row(
                                ActivityType.ISSUE_ASSIGNED,
                                Map.of("actorName", "Kim", "assigneeName", "Lee"),
                                Map.of())),
                        "next-token",
                        true));

        CursorPage<ActivityTool.ActivityEntry> page = tool.getIssueActivity("PROJ-1", null, null);

        assertThat(page.content()).singleElement().satisfies(entry -> {
            assertThat(entry.summary()).isEqualTo("Kim assigned the issue to Lee.");
            assertThat(entry.type()).isEqualTo(ActivityType.ISSUE_ASSIGNED);
            assertThat(entry.changes()).isNull();
        });
        assertThat(page.nextCursor()).isEqualTo("next-token");
        assertThat(page.hasNext()).isTrue();
    }

    @Test
    @DisplayName("success: a field edit carries its diff lines alongside the sentence")
    void fieldEditCarriesItsDiff() {
        authenticate();
        given(activityLogQueryUseCase.getIssueActivities(any(), anyLong(), any(), anyInt()))
                .willReturn(CursorPage.of(
                        List.of(row(
                                ActivityType.ISSUE_UPDATED,
                                Map.of("actorName", "Kim"),
                                Map.of("priority", new FieldChange("P2", "P0")))),
                        null));

        CursorPage<ActivityTool.ActivityEntry> page = tool.getIssueActivity("PROJ-1", null, null);

        assertThat(page.content()).singleElement().satisfies(entry -> {
            assertThat(entry.summary()).isEqualTo("Kim updated the issue.");
            assertThat(entry.changes()).containsExactly("priority: P2 -> P0");
        });
    }

    @Test
    @DisplayName("success: the issue key and cursor reach the use case for the calling agent")
    void passesTheRequestThrough() {
        authenticate();
        given(activityLogQueryUseCase.getIssueActivities(any(), anyLong(), any(), anyInt()))
                .willReturn(CursorPage.empty());

        tool.getIssueActivity("PROJ-1", null, "cursor-token");

        then(activityLogQueryUseCase)
                .should()
                .getIssueActivities(
                        org.mockito.ArgumentMatchers.eq(IssueIdentifier.ofIssueKey("PROJ-1")),
                        org.mockito.ArgumentMatchers.eq(7L),
                        org.mockito.ArgumentMatchers.eq("cursor-token"),
                        anyInt());
    }

    @Test
    @DisplayName("success: an unbounded limit cannot pull a whole history into the agent's context")
    void limitIsBounded() {
        authenticate();
        given(activityLogQueryUseCase.getIssueActivities(any(), anyLong(), any(), anyInt()))
                .willReturn(CursorPage.empty());

        tool.getIssueActivity("PROJ-1", 5000, null);
        assertThat(limitAskedFor()).isEqualTo(100);
    }

    @Test
    @DisplayName("success: an absent or nonsense limit falls back to a page rather than to zero rows")
    void limitFallsBackToADefault() {
        authenticate();
        given(activityLogQueryUseCase.getIssueActivities(any(), anyLong(), any(), anyInt()))
                .willReturn(CursorPage.empty());

        tool.getIssueActivity("PROJ-1", null, null);
        assertThat(limitAskedFor()).isEqualTo(20);

        tool.getIssueActivity("PROJ-1", 0, null);
        assertThat(limitAskedFor()).isEqualTo(20);

        tool.getIssueActivity("PROJ-1", -3, null);
        assertThat(limitAskedFor()).isEqualTo(20);
    }

    private int limitAskedFor() {
        ArgumentCaptor<Integer> limit = ArgumentCaptor.forClass(Integer.class);
        then(activityLogQueryUseCase)
                .should(org.mockito.Mockito.atLeastOnce())
                .getIssueActivities(any(), anyLong(), any(), limit.capture());
        return limit.getValue();
    }

    private static ActivityLogResponse row(
            ActivityType type, Map<String, String> data, Map<String, FieldChange> changes) {
        return ActivityLogResponse.builder()
                .id(1L)
                .eventId(UUID.nameUUIDFromBytes(new byte[] {1}))
                .type(type)
                .data(data)
                .changes(changes)
                .actorMemberId(7L)
                .occurredAt(Instant.EPOCH)
                .build();
    }

    private void authenticate() {
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("SCOPE_READ"));
        MemberDetails principal = new MemberDetails(7L, "agent@tissue.com", "agent", authorities);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }
}
