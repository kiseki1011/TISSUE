package com.tissue.workflow.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.project.application.service.authorization.ProjectAuthExpressions;

@Transactional
public interface WorkflowGraphReplaceUseCase {

	@PreAuthorize(ProjectAuthExpressions.REQUIRES_PROJECT_MEMBER)
	void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd);
}
