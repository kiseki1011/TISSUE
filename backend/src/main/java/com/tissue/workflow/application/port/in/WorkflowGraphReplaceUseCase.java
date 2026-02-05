package com.tissue.workflow.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;

public interface WorkflowGraphReplaceUseCase {

    void replaceWorkflowGraph(Long workflowId, ReplaceWorkflowGraphCommand cmd, ProjectMemberContext actorContext);
}
