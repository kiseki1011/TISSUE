package com.tissue.workflow.application.port.in;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.workflow.application.dto.response.WorkflowDetail;
import com.tissue.workflow.application.dto.response.WorkflowSummary;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(ProjectMemberContext actorContext);

    WorkflowDetail getWorkflowDetail(Long workflowId, ProjectMemberContext actorContext);
}
