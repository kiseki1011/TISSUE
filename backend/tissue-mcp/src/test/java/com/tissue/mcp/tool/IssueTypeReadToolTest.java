package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.tissue.feature.issuetype.application.port.usecase.IssueTypeQueryUseCase;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;

class IssueTypeReadToolTest {

    @Test
    @DisplayName("success: get_issue_types is exposed as an MCP tool")
    void registersGetIssueTypesAsMcpTool() {
        List<Object> toolBeans = List.of(new IssueTypeReadTool(mock(IssueTypeQueryUseCase.class)));

        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(toolBeans);

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("get_issue_types");
    }
}
