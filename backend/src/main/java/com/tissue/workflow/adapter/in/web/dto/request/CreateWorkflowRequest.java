package com.tissue.workflow.adapter.in.web.dto.request;

import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.NodeIdentifier;
import com.tissue.workflow.application.dto.NodeIdentifier.TempKey;
import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;
import com.tissue.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.workflow.domain.enums.StateCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record CreateWorkflowRequest(
        @NotBlank @Size(max = 32) String name,
        @Nullable @Size(max = 255) String description,
        @NotNull ColorType color,
        @NotEmpty List<CreateStatusRequest> createStatusRequests,
        @NotEmpty List<CreateTransitionRequest> createTransitionRequests) {

    public record CreateStatusRequest(
            @NotBlank String tempKey,
            @NotBlank @Size(max = 32) String name,
            @Nullable @Size(max = 255) String description,
            @NotNull ColorType color,
            @NotNull StateCategory category) {}

    public record CreateTransitionRequest(
            @NotBlank @Size(max = 32) String name,
            @Nullable @Size(max = 255) String description,
            @NotBlank String sourceTempKey,
            @NotBlank String targetTempKey) {}

    public CreateWorkflowCommand toCommand(ProjectMemberContext actorContext) {
        List<StateDefinition> stateDefinitions = createStatusRequests.stream()
                .map(s -> new StateDefinition(
                        new NodeIdentifier.TempKey(s.tempKey()),
                        Name.of(s.name()),
                        s.description(),
                        s.color(),
                        s.category))
                .toList();

        List<TransitionDefinition> transitionCommands = createTransitionRequests.stream()
                .map(t -> new TransitionDefinition(
                        new TempKey("trans-" + t.sourceTempKey() + "-to-" + t.targetTempKey()),
                        Name.of(t.name()),
                        t.description(),
                        new TempKey(t.sourceTempKey()),
                        new TempKey(t.targetTempKey())))
                .toList();

        return CreateWorkflowCommand.builder()
                .name(Name.of(name))
                .description(description)
                .color(color)
                .stateDefinitions(stateDefinitions)
                .transitionDefinitions(transitionCommands)
                .actorContext(actorContext)
                .build();
    }
}
