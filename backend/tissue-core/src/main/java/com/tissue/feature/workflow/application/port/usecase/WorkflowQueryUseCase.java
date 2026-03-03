package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(ProjectIdentifier projectIdentifier, Long actorMemberId);

    WorkflowDetail getWorkflowDetail(ProjectIdentifier projectIdentifier, Long workflowId, Long actorMemberId);
}
