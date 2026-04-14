package com.tissue.feature.workflow.web.request;

import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowVcsSettingsCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

public record UpdateWorkflowVcsSettingsRequest(
        @Schema(description = "Id of workflow transition to execute when a PR is opened") @Nullable
        Long vcsPrOpenedTransitionId,

        @Schema(description = "Id of workflow transition to execute when a PR is merged") @Nullable
        Long vcsPrMergedTransitionId) {

    public UpdateWorkflowVcsSettingsCommand toCommand() {
        return new UpdateWorkflowVcsSettingsCommand(vcsPrOpenedTransitionId, vcsPrMergedTransitionId);
    }
}
