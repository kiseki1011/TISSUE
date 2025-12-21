package com.tissue.workflow.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.security.authorization.project.ProjectSecurityExpressions;

@Transactional
public interface WorkflowGraphReplaceUseCase {

	@PreAuthorize(ProjectSecurityExpressions.REQUIRES_PROJECT_MEMBER)
	void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd);
}
