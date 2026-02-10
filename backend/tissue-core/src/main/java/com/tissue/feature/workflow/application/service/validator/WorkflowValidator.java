package com.tissue.feature.workflow.application.service.validator;

import static com.tissue.feature.workflow.domain.enums.StateCategory.COMPLETED;

import com.tissue.feature.issue.application.port.out.IssueQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.port.out.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.exception.DuplicateGuardTypeException;
import com.tissue.feature.workflow.domain.exception.DuplicateWorkflowNameException;
import com.tissue.feature.workflow.domain.exception.WorkflowStateInUseException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.vo.Name;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
            throw new DuplicateWorkflowNameException(name.getNormalized(), project.getKey(), project.getWorkspaceKey());
        }
    }

    public void ensureWorkflowDeletable(Workflow workflow) {
        List<Long> stateIds =
                workflow.getStates().stream().map(WorkflowState::getId).toList();

        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            throw new WorkflowStateInUseException("Workflow is in use by issues in states: " + usedStateIds);
        }
    }

    public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
        List<WorkflowState> statesToCheck = statesToDelete.stream()
                .filter(state -> !state.isCategorizedAs(COMPLETED))
                .toList();

        if (statesToCheck.isEmpty()) {
            return;
        }

        List<Long> stateIds = statesToCheck.stream().map(WorkflowState::getId).toList();

        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            String usedStateNames = statesToCheck.stream()
                    .filter(s -> usedStateIds.contains(s.getId()))
                    .map(WorkflowState::getDisplayName)
                    .collect(Collectors.joining(", "));

            throw new WorkflowStateInUseException(usedStateNames);
        }
    }

    public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
        boolean dup = !usedTypes.add(g.guardType());
        if (dup) {
            throw new DuplicateGuardTypeException(g.guardType());
        }
    }
}
