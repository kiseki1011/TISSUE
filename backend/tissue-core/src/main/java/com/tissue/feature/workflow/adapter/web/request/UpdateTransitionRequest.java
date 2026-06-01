package com.tissue.feature.workflow.adapter.web.request;

import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.JsonNullables;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTransitionRequest(
        @Schema(minLength = NAME_MIN_LENGTH, maxLength = NAME_MAX_LENGTH)
        JsonNullable<@NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) String> name,

        @Schema(maxLength = DESCRIPTION_MAX_LENGTH)
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description) {

    public UpdateTransitionCommand toCommand() {
        return UpdateTransitionCommand.builder()
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .build();
    }
}
