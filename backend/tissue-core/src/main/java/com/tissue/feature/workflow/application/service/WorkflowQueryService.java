package com.tissue.feature.workflow.application.service;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.workflow.application.dto.response.WorkflowDetail;
import com.tissue.feature.workflow.application.dto.response.WorkflowStateCounts;
import com.tissue.feature.workflow.application.dto.response.WorkflowSummary;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.port.usecase.WorkflowQueryUseCase;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.application.service.validator.WorkflowValidator;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.shared.vo.Name;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkflowQueryService implements WorkflowQueryUseCase {

    private final MemberFinder memberFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowRepository workflowRepository;
    private final IssueQueryRepository issueQueryRepository;
    private final WorkflowValidator workflowValidator;

    @Override
    public List<WorkflowSummary> getWorkflows(Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        List<Workflow> workflows = workflowRepository.findAllByOrderByName();

        return workflows.stream().map(WorkflowSummary::from).toList();
    }

    @Override
    public WorkflowDetail getWorkflowDetail(Long workflowId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        Workflow workflow = workflowFinder.getById(workflowId);

        return WorkflowDetail.from(workflow);
    }

    /**
     * Returns active issue counts per state of the given workflow
     */
    @Override
    public WorkflowStateCounts getWorkflowStateCounts(Long workflowId, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        Workflow workflow = workflowFinder.getById(workflowId);

        List<Long> stateIds =
                workflow.getActiveStates().stream().map(WorkflowState::getId).toList();

        List<IssueCountProjection> projections = issueQueryRepository.findActiveIssueCounts(stateIds);

        return WorkflowStateCounts.from(workflowId, projections);
    }

    @Override
    public void checkStateNameUniqueness(Long workflowId, String name, Long actorMemberId) {
        memberFinder.getActiveById(actorMemberId);

        Workflow workflow = workflowFinder.getById(workflowId);

        workflowValidator.ensureStateNameUniqueInWorkflow(workflow, Name.of(name));
    }
}
