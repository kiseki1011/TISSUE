package com.tissue.feature.workflow.application.service.validator;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_GUARD_TYPE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_STATE_IN_USE;

import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.exception.WorkflowStateInUseException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

    private final WorkflowRepository workflowQueryRepository;
    private final IssueQueryRepository issueRepository;

    public void ensureNameUnique(Project project, Name name) {
        boolean dup = workflowQueryRepository.existsByProjectAndName_Normalized(project, name.getNormalized());
        if (dup) {
            throw new ResourceConflictException(DUPLICATE_WORKFLOW_NAME);
        }
    }

    public void ensureWorkflowDeletable(Workflow workflow) {
        List<Long> stateIds =
                workflow.getStates().stream().map(WorkflowState::getId).toList();
        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            throw new BadRequestException(WORKFLOW_STATE_IN_USE);
        }
    }

    public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
        if (statesToDelete.isEmpty()) {
            return;
        }

        List<Long> stateIds =
                statesToDelete.stream().map(WorkflowState::getId).toList();
        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            List<String> usedStateNames = statesToDelete.stream()
                    .filter(s -> usedStateIds.contains(s.getId()))
                    .map(WorkflowState::getDisplayName)
                    .toList();

            throw new WorkflowStateInUseException(usedStateNames);
        }
    }

    public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
        boolean dup = !usedTypes.add(g.guardType());
        if (dup) {
            throw new ResourceConflictException(DUPLICATE_GUARD_TYPE);
        }
    }
}
