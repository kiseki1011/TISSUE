package com.tissue.feature.agent.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.DESCRIPTION_MAX_LENGTH;

import com.tissue.feature.agent.application.dto.PatchAgentCommand;
import com.tissue.feature.agent.domain.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateAgentRequest(
        JsonNullable<AgentType> agentType,

        JsonNullable<Long> modelId,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description) {

    public PatchAgentCommand toCommand() {
        return PatchAgentCommand.builder()
                .agentType(agentType)
                .modelId(modelId)
                .description(description)
                .build();
    }
}
