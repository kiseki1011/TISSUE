package com.tissue.feature.workflow.web.request;

import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowVcsSettingsCommand;
import org.jspecify.annotations.Nullable;

public record UpdateWorkflowVcsSettingsRequest(
        @Nullable Long vcsPrOpenedTransitionId, @Nullable Long vcsPrMergedTransitionId) {

    public UpdateWorkflowVcsSettingsCommand toCommand() {
        return new UpdateWorkflowVcsSettingsCommand(vcsPrOpenedTransitionId, vcsPrMergedTransitionId);
    }
}
