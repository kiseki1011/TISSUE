package com.tissue.feature.agent.adapter.web.request;

import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_MIN_LENGTH;
import static com.tissue.feature.member.domain.policy.MemberConstraintPolicy.NAME_REGEX;

import com.tissue.feature.agent.application.dto.CreateAgentCommand;
import com.tissue.feature.agent.domain.AgentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.Nullable;

public record CreateAgentRequest(
        @Schema(description = "Owner facing agent name (must be unique among your agents)", example = "Backend Bot")
        @NotBlank
        @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        @Pattern(regexp = NAME_REGEX)
        String name,

        @Schema(description = "Functional category of the agent (defaults to GENERAL)", example = "DEVELOPMENT")
        @Nullable
        AgentType agentType,

        @Schema(description = "Id of the AI model catalog entry the agent runs", example = "12") @Nullable
        Long modelId,

        @Schema(description = "Free-form description of the agent", example = "Reviews pull requests and files bugs")
        @Nullable
        @Size(max = DESCRIPTION_MAX_LENGTH)
        String description) {

    public CreateAgentCommand toCommand() {
        return CreateAgentCommand.builder()
                .name(name)
                .agentType(agentType)
                .modelId(modelId)
                .description(description)
                .build();
    }
}
