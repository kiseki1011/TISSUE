package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.shared.dto.ProjectIdentifier;

public interface WorkflowGraphReplaceUseCase {

    void replaceWorkflowGraph(
            ProjectIdentifier pid, Long workflowId, ReplaceWorkflowGraphCommand cmd, Long actorMemberId);
}
