package com.tissue.feature.workflow.adapter.web.request;

import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workflow.application.dto.CreateStateDefinition;
import com.tissue.feature.workflow.application.dto.CreateTransitionDefinition;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.vo.Name;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

@Schema(description = "Request to create a new workflow with statuses and transitions.", example = """
        {
          "name": "Standard Workflow",
          "description": "Default workflow for tracking issues from creation to completion",
          "color": "ORANGE",
          "createStatusRequests": [
            {
              "tempKey": "s-1",
              "name": "To Do",
              "description": "Waiting to be picked up",
              "color": "GREEN",
              "category": "INITIAL"
            },
            {
              "tempKey": "s-2",
              "name": "In Progress",
              "description": "Currently being worked on",
              "color": "BLUE",
              "category": "ACTIVE"
            },
            {
              "tempKey": "s-3",
              "name": "In Review",
              "description": "Awaiting review before completion",
              "color": "YELLOW",
              "category": "ACTIVE"
            },
            {
              "tempKey": "s-4",
              "name": "Done",
              "description": "Completed and closed",
              "color": "PURPLE",
              "category": "COMPLETED"
            }
          ],
          "createTransitionRequests": [
            {
              "name": "Start",
              "description": "Begin working on the issue",
              "sourceTempKey": "s-1",
              "targetTempKey": "s-2"
            },
            {
              "name": "Request Review",
              "description": "Submit for review",
              "sourceTempKey": "s-2",
              "targetTempKey": "s-3"
            },
            {
              "name": "Finish",
              "description": "Complete and close the issue",
              "sourceTempKey": "s-3",
              "targetTempKey": "s-4"
            },
            {
              "name": "Finish",
              "description": "Complete and close the issue",
              "sourceTempKey": "s-2",
              "targetTempKey": "s-4"
            }
          ]
        }""")
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
