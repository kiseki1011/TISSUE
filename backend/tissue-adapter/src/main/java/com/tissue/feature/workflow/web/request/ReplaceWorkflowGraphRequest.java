package com.tissue.feature.workflow.web.request;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.MISSING_NODE_IDENTIFIER;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workflow.application.dto.NodeIdentifier;
import com.tissue.feature.workflow.application.dto.StateDefinition;
import com.tissue.feature.workflow.application.dto.StateMigrationMapping;
import com.tissue.feature.workflow.application.dto.TransitionDefinition;
import com.tissue.feature.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.WorkflowErrorCode;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The request DTO for replacing the entire workflow graph (states + transitions) in a single operation.
 *
 * <p>Describes the full desired graph. Existing nodes use {@code id}. New nodes use
 * a client-generated {@code tempKey}. Nodes not included are deleted.
 * New states require {@code name}, {@code color}. New transitions require {@code name}.
 *
 * <p>When deleted states have active issues, {@code stateMigrationRequests} must map each
 * state (the state to delete) to a target (existing {@code toStateId} or new {@code toTempKey}).
 * Missing mappings result in {@link  WorkflowErrorCode#STATE_MIGRATION_REQUIRED}
 * with per-state issue counts.
 */
@Schema(
        description = "Request to replace the entire workflow graph (statuses and transitions) in a single operation.",
        example = """
        {
          "version": 1,
          "replaceStatusRequests": [
            {
              "id": 10,
              "name": "To Do",
              "description": "Waiting to be picked up",
              "color": "GREEN",
              "category": "INITIAL"
            },
            {
              "id": 11,
              "name": "In Progress",
              "description": "Currently being worked on",
              "color": "BLUE",
              "category": "ACTIVE"
            },
            {
              "tempKey": "s-new-1",
              "name": "In Review",
              "description": "Awaiting review before completion",
              "color": "YELLOW",
              "category": "ACTIVE"
            },
            {
              "id": 13,
              "name": "Done",
              "description": "Completed and closed",
              "color": "PURPLE",
              "category": "COMPLETED"
            }
          ],
          "replaceTransitionRequests": [
            {
              "id": 20,
              "name": "Start",
              "description": "Begin working on the issue",
              "source": { "id": 10 },
              "target": { "id": 11 }
            },
            {
              "tempKey": "t-new-1",
              "name": "Request Review",
              "description": "Submit for review",
              "source": { "id": 11 },
              "target": { "tempKey": "s-new-1" }
            },
            {
              "tempKey": "t-new-2",
              "name": "Approve",
              "description": "Approve and complete the issue",
              "source": { "tempKey": "s-new-1" },
              "target": { "id": 13 }
            }
          ],
          "stateMigrationRequests": [
            {
              "fromStateId": 12,
              "toTempKey": "s-new-1"
            }
          ]
        }""")
public record ReplaceWorkflowGraphRequest(
        @NotNull Long version,
        @NotEmpty @Size(max = 20) List<ReplaceStatusRequest> replaceStatusRequests,
        @NotEmpty @Size(max = 50) List<ReplaceTransitionRequest> replaceTransitionRequests,
        @Nullable @Size(max = 20) List<StateMigrationRequest> stateMigrationRequests) {

    public record ReplaceStatusRequest(
            @Nullable Long id,
            @Nullable String tempKey,

            @Nullable @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
            String name,

            @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
            @Nullable ColorType color,
            @NotNull StateCategory category) {

        NodeIdentifier toIdentifier() {
            if (id != null) {
                return new NodeIdentifier.ExistingId(id);
            }
            if (tempKey != null) {
                return new NodeIdentifier.TempKey(tempKey);
            }
            throw new BadRequestException(MISSING_NODE_IDENTIFIER);
        }
    }

    public record ReplaceTransitionRequest(
            @Nullable Long id,
            @Nullable String tempKey,

            @Nullable @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH)
            String name,

            @Nullable @Size(max = DESCRIPTION_MAX_LENGTH) String description,
            @NotNull Ref source,
            @NotNull Ref target) {

        public record Ref(@Nullable Long id, @Nullable String tempKey) {
            NodeIdentifier toIdentifier() {
                if (id != null) {
                    return new NodeIdentifier.ExistingId(id);
                }
                if (tempKey != null) {
                    return new NodeIdentifier.TempKey(tempKey);
                }
                throw new BadRequestException(MISSING_NODE_IDENTIFIER);
            }
        }

        NodeIdentifier toIdentifier() {
            if (id != null) {
                return new NodeIdentifier.ExistingId(id);
            }
            if (tempKey != null) {
                return new NodeIdentifier.TempKey(tempKey);
            }
            throw new BadRequestException(MISSING_NODE_IDENTIFIER);
        }
    }

    public record StateMigrationRequest(
            @NotNull Long fromStateId,
            @Nullable Long toStateId,
            @Nullable String toTempKey) {

        NodeIdentifier toTargetIdentifier() {
            if (toStateId != null) {
                return new NodeIdentifier.ExistingId(toStateId);
            }
            if (toTempKey != null) {
                return new NodeIdentifier.TempKey(toTempKey);
            }
            throw new BadRequestException(MISSING_NODE_IDENTIFIER);
        }
    }

    public ReplaceWorkflowGraphCommand toCommand() {
        return new ReplaceWorkflowGraphCommand(
                version,
                replaceStatusRequests.stream()
                        .map(s -> StateDefinition.builder()
                                .identifier(s.toIdentifier())
                                .name(s.name() != null ? Name.of(s.name()) : null)
                                .description(s.description())
                                .color(s.color())
                                .category(s.category())
                                .build())
                        .toList(),
                replaceTransitionRequests.stream()
                        .map(t -> TransitionDefinition.builder()
                                .identifier(t.toIdentifier())
                                .name(t.name() != null ? Name.of(t.name()) : null)
                                .description(t.description())
                                .sourceIdentifier(t.source.toIdentifier())
                                .targetIdentifier(t.target.toIdentifier())
                                .build())
                        .toList(),
                stateMigrationRequests != null
                        ? stateMigrationRequests.stream()
                                .map(m -> new StateMigrationMapping(m.fromStateId(), m.toTargetIdentifier()))
                                .toList()
                        : List.of());
    }
}
