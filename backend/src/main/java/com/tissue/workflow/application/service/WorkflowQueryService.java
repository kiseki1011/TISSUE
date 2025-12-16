package com.tissue.workflow.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tissue.issue.application.dto.IssueCountProjection;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.dto.response.WorkflowDetail;
import com.tissue.workflow.application.dto.response.WorkflowSummary;
import com.tissue.workflow.application.port.in.WorkflowQueryUseCase;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowQueryService implements WorkflowQueryUseCase {

	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowQueryRepository workflowQueryRepository;
	private final IssueQueryRepository issueQueryRepository;

	@Override
	public List<WorkflowSummary> getWorkflows(String workspaceKey, String projectKey, boolean includeArchived) {
		Project project = projectFinder.findBy(projectKey, workspaceKey);

		List<Workflow> workflows;
		if (includeArchived) {
			workflows = workflowQueryRepository.findAllByProjectOrderByLabel(project);
		} else {
			workflows = workflowQueryRepository.findAllByProjectAndArchivedFalseOrderByLabel(project);
		}

		return workflows.stream()
			.map(WorkflowSummary::from)
			.toList();
	}

	@Override
	public WorkflowDetail getWorkflowDetail(String workspaceKey, String projectKey, Long workflowId) {
		Project project = projectFinder.findBy(projectKey, workspaceKey);
		Workflow workflow = workflowFinder.findBy(workflowId, project);

		List<Long> stateIds = workflow.getActiveStates().stream()
			.map(WorkflowState::getId)
			.toList();

		List<IssueCountProjection> projections = issueQueryRepository.findActiveIssueCounts(stateIds);

		return WorkflowDetail.of(workflow, projections);
	}
}
