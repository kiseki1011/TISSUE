package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowStateCounts;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.shared.dto.ProjectIdentifier;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(ProjectIdentifier pid, Long actorMemberId);

    WorkflowDetail getWorkflowDetail(ProjectIdentifier pid, Long workflowId, Long actorMemberId);

    WorkflowStateCounts getWorkflowStateCounts(ProjectIdentifier pid, Long workflowId, Long actorMemberId);

    void checkStateNameUniqueness(ProjectIdentifier pid, Long workflowId, String name, Long actorMemberId);
}
