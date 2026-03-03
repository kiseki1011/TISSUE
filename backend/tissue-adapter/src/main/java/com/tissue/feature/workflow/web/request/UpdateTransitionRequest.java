package com.tissue.feature.workflow.web.request;

import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.DESCRIPTION_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MAX_LENGTH;
import static com.tissue.feature.workflow.domain.policy.WorkflowConstraintPolicy.NAME_MIN_LENGTH;

import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.shared.vo.Name;
import com.tissue.support.util.JsonNullables;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.openapitools.jackson.nullable.JsonNullable;

public record UpdateTransitionRequest(
        JsonNullable<@NotBlank @Size(min = NAME_MIN_LENGTH, max = NAME_MAX_LENGTH) String> name,
        JsonNullable<@Size(max = DESCRIPTION_MAX_LENGTH) String> description) {

    public UpdateTransitionCommand toCommand() {
        return UpdateTransitionCommand.builder()
                .name(JsonNullables.map(name, Name::of))
                .description(description)
                .build();
    }
}
