package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issue.application.port.usecase.IssueQueryUseCase;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;

class IssueReadToolTest {

    @Test
    @DisplayName("success: the read methods are exposed as MCP tools")
    void registersReadMethodsAsMcpTools() {
        List<Object> toolBeans = List.of(new IssueReadTool(mock(IssueQueryUseCase.class)));

        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(toolBeans);

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("get_issue", "list_available_transitions");
    }
}
