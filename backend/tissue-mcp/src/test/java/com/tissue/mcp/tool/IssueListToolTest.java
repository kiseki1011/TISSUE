package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.application.port.usecase.IssueListQueryUseCase;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;

class IssueListToolTest {

    @Test
    @DisplayName("success: the discover work methods are exposed as MCP tools")
    void registersDiscoverToolsAsMcpTools() {
        List<Object> toolBeans = List.of(new IssueListTool(mock(IssueListQueryUseCase.class)));

        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(toolBeans);

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("get_my_work", "get_backlog", "get_current_sprint_issues");
    }
}
