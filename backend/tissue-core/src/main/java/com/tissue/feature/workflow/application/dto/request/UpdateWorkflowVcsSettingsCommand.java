package com.tissue.feature.workflow.application.dto.request;

import org.jspecify.annotations.Nullable;

public record UpdateWorkflowVcsSettingsCommand(
        @Nullable Long vcsPrOpenedTransitionId, @Nullable Long vcsPrMergedTransitionId) {}
