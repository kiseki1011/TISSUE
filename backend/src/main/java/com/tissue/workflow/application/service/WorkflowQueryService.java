package com.tissue.workflow.application.service;

import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.dto.response.WorkflowDetail;
import com.tissue.workflow.application.dto.response.WorkflowSummary;
import com.tissue.workflow.application.port.in.WorkflowQueryUseCase;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
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
    private final WorkflowQueryRepository workflowQueryRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public List<WorkflowSummary> getWorkflows(ProjectMemberContext actorContext) {
        projectAuthService.requireProjectViewer(actorContext);
        Project project = projectFinder.getBy(actorContext.projectId());

        List<Workflow> workflows = workflowQueryRepository.findAllByProjectOrderByLabel(project);

        return workflows.stream().map(WorkflowSummary::from).toList();
    }

    @Override
    public WorkflowDetail getWorkflowDetail(Long workflowId, ProjectMemberContext actorContext) {
        projectAuthService.requireProjectViewer(actorContext);
        Project project = projectFinder.getBy(actorContext.projectId());
        Workflow workflow = workflowFinder.getBy(workflowId, project);

        List<Long> stateIds =
                workflow.getActiveStates().stream().map(WorkflowState::getId).toList();

        List<IssueCountProjection> projections = issueQueryRepository.findActiveIssueCounts(stateIds);

        return WorkflowDetail.of(workflow, projections);
    }
}
