package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(ProjectMemberContext actorContext);

    WorkflowDetail getWorkflowDetail(Long workflowId, ProjectMemberContext actorContext);
}
