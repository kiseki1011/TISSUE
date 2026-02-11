package com.tissue.feature.workflow.application.service;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.dto.ProjectMemberContext;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkflowQueryService implements WorkflowQueryUseCase {

    private final ProjectFinder projectFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowRepository workflowQueryRepository;
    private final IssueQueryRepository issueQueryRepository;

    @Override
    public List<WorkflowSummary> getWorkflows(ProjectMemberContext actorContext) {
        Project project = projectFinder.getBy(actorContext.workspaceKey(), actorContext.projectKey());

        List<Workflow> workflows = workflowQueryRepository.findAllByProjectOrderByLabel(project);

        return workflows.stream().map(WorkflowSummary::from).toList();
    }

    @Override
    public WorkflowDetail getWorkflowDetail(Long workflowId, ProjectMemberContext actorContext) {
        Workflow workflow =
                workflowFinder.getWithProjectBy(actorContext.workspaceKey(), actorContext.projectKey(), workflowId);

        List<Long> stateIds =
                workflow.getActiveStates().stream().map(WorkflowState::getId).toList();

        List<IssueCountProjection> projections = issueQueryRepository.findActiveIssueCounts(stateIds);

        return WorkflowDetail.of(workflow, projections);
    }
}
