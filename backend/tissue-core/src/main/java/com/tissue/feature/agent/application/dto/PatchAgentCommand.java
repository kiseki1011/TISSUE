package com.tissue.feature.agent.application.dto;

import com.tissue.feature.agent.domain.AgentType;
import lombok.Builder;
import org.openapitools.jackson.nullable.JsonNullable;

@Builder
public record PatchAgentCommand(
        JsonNullable<AgentType> agentType, JsonNullable<Long> modelId, JsonNullable<String> description) {}
