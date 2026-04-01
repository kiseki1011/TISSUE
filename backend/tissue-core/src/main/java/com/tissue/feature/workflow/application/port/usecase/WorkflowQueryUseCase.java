package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(ProjectIdentifier pid, Long actorMemberId);

    WorkflowDetail getWorkflowDetail(String workspaceKey, Long workflowId, Long actorMemberId);

    void checkStateNameUniqueness(String workspaceKey, Long workflowId, String name, Long actorMemberId);
}
