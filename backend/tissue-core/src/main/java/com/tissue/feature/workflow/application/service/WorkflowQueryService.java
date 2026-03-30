package com.tissue.feature.workflow.application.service;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.application.service.validator.WorkflowValidator;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.vo.Name;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkflowQueryService implements WorkflowQueryUseCase {

    private final ProjectFinder projectFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowRepository workflowRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final WorkflowValidator workflowValidator;

    @Override
    public List<WorkflowSummary> getWorkflows(ProjectIdentifier pid, Long actorMemberId) {
        Project project = projectFinder.getBy(pid.workspaceKey(), pid.projectKey());
        projectMemberFinder.getBy(project, actorMemberId);

        List<Workflow> workflows = workflowRepository.findAllByProjectOrderByLabel(project);

        return workflows.stream().map(WorkflowSummary::from).toList();
    }

    @Override
    public WorkflowDetail getWorkflowDetail(ProjectIdentifier pid, Long workflowId, Long actorMemberId) {
        Workflow workflow = workflowFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), workflowId);

        projectMemberFinder.getBy(workflow.getProject(), actorMemberId);

        List<Long> stateIds =
                workflow.getActiveStates().stream().map(WorkflowState::getId).toList();

        List<IssueCountProjection> projections = issueQueryRepository.findActiveIssueCounts(stateIds);

        return WorkflowDetail.of(workflow, projections);
    }

    @Override
    public void checkStateNameUniqueness(ProjectIdentifier pid, Long workflowId, String name, Long actorMemberId) {
        Workflow workflow = workflowFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), workflowId);

        projectMemberFinder.getBy(workflow.getProject(), actorMemberId);

        workflowValidator.ensureStateNameUniqueInWorkflow(workflow, Name.of(name));
    }
}
