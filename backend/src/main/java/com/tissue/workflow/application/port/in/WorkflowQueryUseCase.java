package com.tissue.workflow.application.port.in;

import com.tissue.workflow.application.dto.response.WorkflowDetail;
import com.tissue.workflow.application.dto.response.WorkflowSummary;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(String workspaceKey, String projectKey, boolean includeArchived);

    WorkflowDetail getWorkflowDetail(String workspaceKey, String projectKey, Long workflowId);
}
