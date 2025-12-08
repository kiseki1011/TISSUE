package com.tissue.api.workflow.application.port.in;

import static com.tissue.api.security.authorization.ProjectSecurityExpressions.*;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.workflow.application.dto.request.ReplaceWorkflowGraphCommand;

@Transactional
public interface WorkflowGraphReplaceUseCase {

	@PreAuthorize(REQUIRES_PROJECT_WRITER)
	void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd);
}
