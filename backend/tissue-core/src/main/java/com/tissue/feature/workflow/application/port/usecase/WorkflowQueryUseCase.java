package com.tissue.feature.workflow.application.port.usecase;

import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowStateCounts;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import java.util.List;

public interface WorkflowQueryUseCase {

    List<WorkflowSummary> getWorkflows(Long actorMemberId);

    WorkflowDetail getWorkflowDetail(Long workflowId, Long actorMemberId);

    WorkflowStateCounts getWorkflowStateCounts(Long workflowId, Long actorMemberId);

    void checkStateNameUniqueness(Long workflowId, String name, Long actorMemberId);
}
