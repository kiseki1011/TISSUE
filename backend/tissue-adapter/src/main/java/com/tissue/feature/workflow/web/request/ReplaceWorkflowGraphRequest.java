package com.tissue.feature.workflow.web.request;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INVALID_GRAPH_REQUEST;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workflow.application.dto.NodeIdentifier;
import com.tissue.feature.workflow.application.dto.StateDefinition;
import com.tissue.feature.workflow.application.dto.TransitionDefinition;
import com.tissue.feature.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.vo.Name;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
            throw new BadRequestException(INVALID_GRAPH_REQUEST)
                    .addContext("reason", "Either 'id' or 'tempKey' must be provided for state node identifier");
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
                throw new BadRequestException(INVALID_GRAPH_REQUEST)
                        .addContext("reason", "Either 'id' or 'tempKey' must be provided for state node identifier");
            }
        }

        NodeIdentifier toIdentifier() {
            if (id != null) {
                return new NodeIdentifier.ExistingId(id);
            }
            if (tempKey != null) {
                return new NodeIdentifier.TempKey(tempKey);
            }
            throw new BadRequestException(INVALID_GRAPH_REQUEST)
                    .addContext("reason", "Either 'id' or 'tempKey' must be provided for transition node identifier");
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
                        .toList());
    }
}
