package com.tissue.feature.workflow.application.service.validator;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_GUARD_TYPE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_STATE_NAME;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_IN_USE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.WORKFLOW_STATE_IN_USE;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.issuetype.application.port.repository.IssueTypeRepository;
import com.tissue.feature.workflow.application.dto.GuardConfigData;
import com.tissue.feature.workflow.application.dto.NodeIdentifier;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.exception.StateMigrationRequiredException;
import com.tissue.feature.workflow.domain.exception.WorkflowStateInUseException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowValidator {

    private final WorkflowRepository workflowQueryRepository;
    private final IssueQueryRepository issueRepository;
    private final IssueTypeRepository issueTypeRepository;

    public void ensureNameUnique(Name name) {
        boolean dup = workflowQueryRepository.existsByName_NormalizedName(name.getNormalizedName());
        if (dup) {
            throw new ResourceConflictException(DUPLICATE_WORKFLOW_NAME);
        }
    }

    public void ensureWorkflowDeletable(Workflow workflow) {
        if (issueTypeRepository.existsByWorkflow_Id(workflow.getId())) {
            throw new ResourceConflictException(WORKFLOW_IN_USE);
        }

        List<Long> stateIds =
                workflow.getStates().stream().map(WorkflowState::getId).toList();
        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            throw new ResourceConflictException(WORKFLOW_STATE_IN_USE);
        }
    }

    public void ensureStatesDeletable(Set<WorkflowState> statesToDelete) {
        if (statesToDelete.isEmpty()) {
            return;
        }

        List<Long> stateIds = statesToDelete.stream().map(WorkflowState::getId).toList();
        List<Long> usedStateIds = issueRepository.findStateIdsUsedByActiveIssues(stateIds);

        if (!usedStateIds.isEmpty()) {
            List<String> usedStateNames = statesToDelete.stream()
                    .filter(s -> usedStateIds.contains(s.getId()))
                    .map(WorkflowState::getDisplayName)
                    .toList();

            throw new WorkflowStateInUseException(usedStateNames);
        }
    }

    public void ensureMigrationMappingsComplete(
            Set<WorkflowState> statesToDelete,
            List<Long> usedStateIds,
            Map<Long, NodeIdentifier> migrationMap,
            List<IssueCountProjection> issueCounts) {
        Map<Long, Long> issueCountsByStateId = issueCounts.stream()
                .collect(Collectors.toMap(IssueCountProjection::getStateId, IssueCountProjection::getCount));

        List<StateMigrationRequiredException.Detail> missing = usedStateIds.stream()
                .filter(id -> !migrationMap.containsKey(id))
                .map(id -> {
                    String name = statesToDelete.stream()
                            .filter(s -> s.getId().equals(id))
                            .findFirst()
                            .map(WorkflowState::getDisplayName)
                            .orElse("unknown");
                    long count = issueCountsByStateId.getOrDefault(id, 0L);
                    return new StateMigrationRequiredException.Detail(id, name, count);
                })
                .toList();

        if (!missing.isEmpty()) {
            throw new StateMigrationRequiredException(missing);
        }
    }

    public void ensureStateNameUniqueInWorkflow(Workflow workflow, Name name) {
        if (workflow.hasStateWithName(name)) {
            throw new ResourceConflictException(DUPLICATE_STATE_NAME);
        }
    }

    public void ensureNoDuplicateGuard(GuardConfigData g, Set<GuardType> usedTypes) {
        boolean dup = !usedTypes.add(g.guardType());
        if (dup) {
            throw new ResourceConflictException(DUPLICATE_GUARD_TYPE);
        }
    }
}
