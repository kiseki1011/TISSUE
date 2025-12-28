package com.tissue.workflow.application.service.validator;

import static com.tissue.workflow.domain.enums.StateCategory.*;

import com.tissue.common.vo.Name;
import com.tissue.issue.application.port.out.IssueQueryRepository;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.dto.GuardConfigData;
import com.tissue.workflow.application.port.out.WorkflowQueryRepository;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.exception.WorkflowExceptions;
import com.tissue.workflow.domain.guard.GuardType;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

    private final WorkflowQueryRepository workflowQueryRepository;
    private final IssueQueryRepository issueRepository;

    public void ensureLabelUnique(Project project, Name name) {
        boolean dup =
                workflowQueryRepository.existsByProjectAndName_Normalized(
                        project, name.getNormalized());
        if (dup) {
            throw WorkflowExceptions.duplicateWorkflowName(
                    name.getNormalized(), project.getKey(), project.getWorkspaceKey());
        }
    }

    public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
        List<WorkflowState> statesToCheck =
                statesToDelete.stream().filter(state -> !state.isCategorizedAs(COMPLETED)).toList();

        if (statesToCheck.isEmpty()) {
            return;
        }

        List<Long> stateIds = statesToCheck.stream().map(WorkflowState::getId).toList();

        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            String usedStateNames =
                    statesToCheck.stream()
                            .filter(s -> usedStateIds.contains(s.getId()))
                            .map(WorkflowState::getDisplayName)
                            .collect(Collectors.joining(", "));

            throw WorkflowExceptions.workflowStateInUse(usedStateNames);
        }
    }

    public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
        boolean dup = !usedTypes.add(g.guardType());
        if (dup) {
            throw WorkflowExceptions.duplicateGuardType(g.guardType());
        }
    }
}
