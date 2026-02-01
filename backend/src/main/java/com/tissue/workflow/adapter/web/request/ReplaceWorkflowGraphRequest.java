package com.tissue.workflow.adapter.web.request;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.NodeIdentifier;
import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;
import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workflow.domain.exception.InvalidGraphRequestException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * TODO: needs javadoc
 *  - existing states/transitions pass on IDs, while newly added states/transitions pass on tempKeys
 *  - should the tempKey be created on the client side? also what format should we use? UUID?
 *  or a custom format like "temp-trans-{uuid}"?
 */
public record ReplaceWorkflowGraphRequest(
        @NotNull Long version,
        @NotEmpty List<ReplaceStatusRequest> replaceStatusRequests,
        @NotEmpty List<ReplaceTransitionRequest> replaceTransitionRequests) {

    public record ReplaceStatusRequest(
            @Nullable Long id,
            @Nullable String tempKey,
            @NotNull StateCategory category) {

        NodeIdentifier toIdentifier() {
            if (id != null) {
                return new NodeIdentifier.ExistingId(id);
            }
            if (tempKey != null) {
                return new NodeIdentifier.TempKey(tempKey);
            }
            throw new InvalidGraphRequestException(
                    "Either id or tempKey must be provided for state node identifier",
                    "state_node",
                    "missing_identifier");
        }
    }

    public record ReplaceTransitionRequest(
            @Nullable Long id,
            @Nullable String tempKey,
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
                throw new InvalidGraphRequestException(
                        "Either id or tempKey must be provided for state node identifier",
                        "state_node",
                        "missing_identifier");
            }
        }

        NodeIdentifier toIdentifier() {
            if (id != null) {
                return new NodeIdentifier.ExistingId(id);
            }
            if (tempKey != null) {
                return new NodeIdentifier.TempKey(tempKey);
            }
            throw new InvalidGraphRequestException(
                    "Either id or tempKey must be provided for transition node identifier",
                    "transition_node",
                    "missing_identifier");
        }
    }

    public ReplaceWorkflowGraphCommand toCommand(Long workflowId, ProjectMemberContext actorContext) {
        return new ReplaceWorkflowGraphCommand(
                workflowId,
                version,
                replaceStatusRequests.stream()
                        .map(s -> StateDefinition.builder()
                                .identifier(s.toIdentifier())
                                .category(s.category)
                                .build())
                        .toList(),
                replaceTransitionRequests.stream()
                        .map(t -> TransitionDefinition.builder()
                                .identifier(t.toIdentifier())
                                .sourceIdentifier(t.source.toIdentifier())
                                .targetIdentifier(t.target.toIdentifier())
                                .build())
                        .toList(),
                actorContext);
    }
}
