package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.request.ReplaceWorkflowGraphCommand;

public interface WorkflowGraphReplaceUseCase {

    void replaceWorkflowGraph(Long workflowId, ReplaceWorkflowGraphCommand cmd, Long actorMemberId);
}
