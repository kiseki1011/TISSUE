package com.tissue.mcp.tool;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.mcp.annotation.spring.SyncMcpAnnotationProviders;

@DisplayName("WhoAmITool")
class WhoAmIToolTest {

    @Test
    @DisplayName("success: the @McpTool method is exposed as an MCP tool named 'whoami'")
    void registersWhoamiAsMcpTool() {
        List<Object> toolBeans = List.of(new WhoAmITool());

        List<SyncToolSpecification> specifications = SyncMcpAnnotationProviders.toolSpecifications(toolBeans);

        assertThat(specifications)
                .extracting(specification -> specification.tool().name())
                .contains("whoami");
    }
}
