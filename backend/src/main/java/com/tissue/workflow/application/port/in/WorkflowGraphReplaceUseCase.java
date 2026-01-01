package com.tissue.workflow.application.port.in;

import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;

public interface WorkflowGraphReplaceUseCase {

    void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd);
}
