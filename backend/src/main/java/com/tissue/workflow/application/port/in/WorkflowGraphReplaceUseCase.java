package com.tissue.workflow.application.port.in;

import com.tissue.project.application.service.authorization.ProjectAuthExpressions;
import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import org.springframework.security.access.prepost.PreAuthorize;

public interface WorkflowGraphReplaceUseCase {

    @PreAuthorize(ProjectAuthExpressions.REQUIRES_PROJECT_MEMBER)
    void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd);
}
