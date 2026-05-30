package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.workflow.domain.Workflow;

public record WorkflowCreateResponse(Long workflowId) {
    public static WorkflowCreateResponse from(Workflow workflow) {
        return new WorkflowCreateResponse(workflow.getId());
    }
}
