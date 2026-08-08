package com.tissue.feature.agent.application.dto;

import com.tissue.feature.agent.domain.AgentType;
import lombok.Builder;
import org.jspecify.annotations.Nullable;

@Builder
public record CreateAgentCommand(
        String name,
        @Nullable AgentType agentType,
        @Nullable Long modelId,
        @Nullable String description) {}
