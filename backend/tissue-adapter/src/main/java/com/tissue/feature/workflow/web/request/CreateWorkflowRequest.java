package com.tissue.feature.workflow.web.request;

import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workflow.application.dto.CreateStateDefinition;
import com.tissue.feature.workflow.application.dto.CreateTransitionDefinition;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateWorkflowRequest(
        @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
        String name,

        @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
        @NotNull ColorType color,
        @NotEmpty @Size(max = 20) List<CreateStatusRequest> createStatusRequests,
        @NotEmpty @Size(max = 50) List<CreateTransitionRequest> createTransitionRequests) {

    public record CreateStatusRequest(
            @NotBlank String tempKey,

            @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
            String name,

            @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
            @NotNull ColorType color,
            @NotNull StateCategory category) {}

    public record CreateTransitionRequest(
            @NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
            String name,

            @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
            @NotBlank String sourceTempKey,
            @NotBlank String targetTempKey) {}

    public CreateWorkflowCommand toCommand() {
        List<CreateStateDefinition> stateDefinitions = createStatusRequests.stream()
                .map(s -> new CreateStateDefinition(
                        s.tempKey(), Name.of(s.name()), s.description(), s.color(), s.category()))
                .toList();

        List<CreateTransitionDefinition> transitionDefinitions = createTransitionRequests.stream()
                .map(t -> new CreateTransitionDefinition(
                        Name.of(t.name()), t.description(), t.sourceTempKey(), t.targetTempKey()))
                .toList();

        return CreateWorkflowCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .stateDefinitions(stateDefinitions)
                .transitionDefinitions(transitionDefinitions)
                .build();
    }
}
