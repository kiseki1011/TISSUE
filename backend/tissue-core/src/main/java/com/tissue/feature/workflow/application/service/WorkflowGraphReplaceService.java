package com.tissue.feature.workflow.application.service;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INCOMPLETE_NEW_STATE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INCOMPLETE_NEW_TRANSITION;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INVALID_INITIAL_STATE_COUNT;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.MIGRATION_TARGET_BEING_DELETED;

import com.tissue.feature.issue.application.dto.IssueCountProjection;
import com.tissue.feature.issue.application.port.repository.IssueCommandRepository;
import com.tissue.feature.issue.application.port.repository.IssueQueryRepository;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.dto.NodeIdentifier;
import com.tissue.feature.workflow.application.dto.StateDefinition;
import com.tissue.feature.workflow.application.dto.StateMigrationMapping;
import com.tissue.feature.workflow.application.dto.TransitionDefinition;
import com.tissue.feature.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.feature.workflow.application.port.usecase.WorkflowGraphReplaceUseCase;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.feature.workflow.application.service.validator.WorkflowValidator;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.WorkflowTransitionNotFoundException;
import com.tissue.feature.workflow.domain.exception.WorkflowVersionMismatchException;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Replaces the entire graph topology of a workflow in a single operation.
 *
 * <p>This service handles the complex case where a client submits the full desired graph
 * (states + transitions) and the server diffs it against the current graph. Existing nodes
 * are identified by database ID; new nodes use client generated temporary keys.
 *
 * <p>Optimistic locking via {@code @Version} prevents lost updates when multiple users
 * edit the same workflow concurrently.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
@SuppressWarnings("checkstyle:LineLength")
public class WorkflowGraphReplaceService implements WorkflowGraphReplaceUseCase {

    private final WorkflowFinder workflowFinder;
    private final ProjectMemberFinder projectMemberFinder;
    private final WorkflowGraphValidator graphValidator;
    private final WorkflowValidator workflowValidator;
    private final ProjectAuthorizationService projectAuthService;
    private final IssueQueryRepository issueQueryRepository;
    private final IssueCommandRepository issueCommandRepository;

    /**
     * Replaces the full graph topology of a workflow.
     *
     * <p>The command contains the <b>full desired state</b> of the graph. Existing nodes are
     * referenced by their database IDs; new nodes use client-generated temporary keys.
     * Any existing states/transitions not present in the command are deleted.
     *
     * <p><b>Processing order</b> (order matters for referential integrity):
     * <ol>
     *   <li>Version check — reject if another user modified the workflow</li>
     *   <li>Build state resolver — index existing states by ID, create new states from TempKeys</li>
     *   <li>Sync transitions — delete removed, rewire existing, create new transitions</li>
     *   <li>Apply category changes — update categories for existing states</li>
     *   <li>Set initial state — resolve and assign the single {@link StateCategory#INITIAL} state</li>
     *   <li>Migrate issues — bulk-move issues from states being deleted to mapped targets</li>
     *   <li>Delete removed states — remove states not referenced in the command</li>
     *   <li>Validate graph — validate the final graph structure</li>
     * </ol>
     *
     * <p><b>Example command shape:</b>
     * <pre>{@code
     * {
     *   "version": 3,
     *   "stateDefinitions": [
     *     { "identifier": ExistingId(10), "category": "INITIAL" },
     *     { "identifier": ExistingId(11), "category": "ACTIVE" },
     *     { "identifier": TempKey("new1"), "name": "Review", "color": "YELLOW", "category": "ACTIVE" },
     *     { "identifier": ExistingId(13), "category": "COMPLETED" }
     *   ],
     *   "transitionDefinitions": [
     *     { "identifier": ExistingId(100), "source": ExistingId(10), "target": ExistingId(11) },
     *     { "identifier": TempKey("t1"), "name": "Request Review", "source": ExistingId(11), "target": TempKey("new1") },
     *     { "identifier": TempKey("t2"), "name": "Approve", "source": TempKey("new1"), "target": ExistingId(13) }
     *   ]
     * }
     * }</pre>
     *
     * <p>In this example, state 12 (a new state will replace) and any transitions not listed are deleted.
     * Transition 100 is rewired (source/target may change). New state "Review" and
     * two new transitions are created.
     */
    @Override
    public void replaceWorkflowGraph(
            ProjectIdentifier pid, Long workflowId, ReplaceWorkflowGraphCommand cmd, Long actorMemberId) {
        Workflow workflow = workflowFinder.getWithProjectBy(pid.workspaceKey(), pid.projectKey(), workflowId);

        ProjectMember actor =
                projectMemberFinder.getWithWorkspaceMember(pid.workspaceKey(), pid.projectKey(), actorMemberId);
        projectAuthService.requireProjectManager(actor);

        log.info(
                "Replacing workflow graph: workflowId={}, projectKey={}, version={}, "
                        + "requestedStates={}, requestedTransitions={}, actorMemberId={}",
                workflowId,
                pid.projectKey(),
                cmd.version(),
                cmd.stateDefinitions().size(),
                cmd.transitionDefinitions().size(),
                actorMemberId);

        checkWorkflowVersion(cmd, workflow);

        StateResolver stateResolver = buildStateResolver(workflow, cmd.stateDefinitions());

        syncTransitions(workflow, cmd.transitionDefinitions(), stateResolver);

        applyStateCategoryChanges(workflow, cmd.stateDefinitions(), stateResolver);

        resolveAndSetInitial(workflow, cmd.stateDefinitions(), stateResolver);

        migrateIssuesFromDeletedStates(workflow, cmd, stateResolver);

        deleteRemovedStates(workflow, cmd);

        graphValidator.ensureValidWorkflowGraph(workflow);

        log.info("Workflow graph replacement completed: workflowId={}", workflowId);
    }

    private void checkWorkflowVersion(ReplaceWorkflowGraphCommand cmd, Workflow workflow) {
        if (!Objects.equals(workflow.getVersion(), cmd.version())) {
            throw new WorkflowVersionMismatchException(cmd.version(), workflow.getVersion());
        }
    }

    /**
     * Indexes existing states by ID, and creates new states for TempKey entries.
     * Returns a {@link StateResolver} that can look up any state by its {@link NodeIdentifier}.
     */
    private StateResolver buildStateResolver(Workflow workflow, List<StateDefinition> stateDefinitions) {
        Map<Long, WorkflowState> existingStatuses = new HashMap<>();
        Map<String, WorkflowState> newStatuses = new HashMap<>();

        for (WorkflowState s : workflow.getStates()) {
            existingStatuses.put(s.getId(), s);
        }

        for (var s : stateDefinitions) {
            if (s.identifier() instanceof NodeIdentifier.TempKey(String key)) {
                validateNewStateFields(s);
                WorkflowState created = workflow.addState(
                        Objects.requireNonNull(s.name()),
                        s.description(),
                        Objects.requireNonNull(s.color()),
                        s.category());
                newStatuses.put(key, created);
                log.debug("Created new state: tempKey={}, name={}, category={}", key, s.name(), s.category());
            }
        }

        log.debug("State resolver built: existingStates={}, newStates={}", existingStatuses.size(), newStatuses.size());

        return new StateResolver(existingStatuses, newStatuses);
    }

    /**
     * Synchronizes transitions: deletes those absent from the command, rewires existing ones
     * to new source/target states, and creates new transitions.
     */
    private void syncTransitions(
            Workflow workflow, List<TransitionDefinition> transitionDefinitions, StateResolver stateResolver) {
        deleteRemovedTransitions(workflow, transitionDefinitions);
        Map<Long, WorkflowTransition> existingTransitions = indexExistingTransitions(workflow);

        for (var cmd : transitionDefinitions) {
            WorkflowState src = stateResolver.resolve(cmd.sourceIdentifier());
            WorkflowState trg = stateResolver.resolve(cmd.targetIdentifier());

            if (cmd.identifier() instanceof NodeIdentifier.ExistingId(Long id)) {
                rewireExistingTransition(workflow, id, src, trg, existingTransitions);
                continue;
            }

            validateNewTransitionFields(cmd);
            workflow.addTransition(Objects.requireNonNull(cmd.name()), cmd.description(), src, trg);
            log.debug(
                    "Created new transition: name={}, source={}, target={}",
                    cmd.name(),
                    src.getDisplayName(),
                    trg.getDisplayName());
        }
    }

    private void applyStateCategoryChanges(
            Workflow workflow, List<StateDefinition> stateDefinitions, StateResolver resolver) {
        for (var cmd : stateDefinitions) {
            if (cmd.identifier() instanceof NodeIdentifier.ExistingId) {
                WorkflowState state = resolver.resolve(cmd.identifier());
                if (!state.isCategorizedAs(cmd.category())) {
                    log.debug(
                            "Changing state category: stateId={}, {} -> {}",
                            state.getId(),
                            state.getCategory(),
                            cmd.category());
                }
                workflow.changeStateCategory(state, cmd.category());
            }
        }
    }

    private void resolveAndSetInitial(
            Workflow workflow, List<StateDefinition> stateDefinitions, StateResolver stateResolver) {
        var initialCmds = stateDefinitions.stream()
                .filter(cmd -> cmd.category() == StateCategory.INITIAL)
                .toList();

        if (initialCmds.size() != 1) {
            throw new BadRequestException(INVALID_INITIAL_STATE_COUNT);
        }

        WorkflowState initialState =
                stateResolver.resolve(initialCmds.getFirst().identifier());

        workflow.setInitialState(initialState);
    }

    private void migrateIssuesFromDeletedStates(
            Workflow workflow, ReplaceWorkflowGraphCommand cmd, StateResolver stateResolver) {
        Set<WorkflowState> statesToDelete = findStatesToDelete(workflow, cmd);
        if (statesToDelete.isEmpty()) {
            return;
        }

        List<Long> deleteStateIds =
                statesToDelete.stream().map(WorkflowState::getId).toList();
        List<Long> usedStateIds = issueQueryRepository.findStateIdsUsedByActiveIssues(deleteStateIds);
        if (usedStateIds.isEmpty()) {
            return;
        }

        Map<Long, NodeIdentifier> migrationMap = cmd.stateMigrations().stream()
                .collect(
                        Collectors.toMap(StateMigrationMapping::fromStateId, StateMigrationMapping::toStateIdentifier));

        List<IssueCountProjection> issueCounts = issueQueryRepository.findActiveIssueCounts(deleteStateIds);

        workflowValidator.ensureMigrationMappingsComplete(statesToDelete, usedStateIds, migrationMap, issueCounts);

        Set<Long> deleteStateIdSet = Set.copyOf(deleteStateIds);

        for (var entry : migrationMap.entrySet()) {
            Long fromStateId = entry.getKey();
            if (!usedStateIds.contains(fromStateId)) {
                continue;
            }

            WorkflowState targetState = stateResolver.resolve(entry.getValue());

            if (targetState.getId() != null && deleteStateIdSet.contains(targetState.getId())) {
                throw new BadRequestException(MIGRATION_TARGET_BEING_DELETED);
            }

            int migrated = issueCommandRepository.bulkMigrateCurrentState(fromStateId, targetState.getId());
            log.info("Migrated {} issues from state {} to state {}", migrated, fromStateId, targetState.getId());
        }
    }

    private void deleteRemovedStates(Workflow workflow, ReplaceWorkflowGraphCommand cmd) {
        Set<WorkflowState> toDelete = findStatesToDelete(workflow, cmd);

        if (!toDelete.isEmpty()) {
            workflowValidator.ensureStatesDeletable(toDelete);
            log.debug(
                    "Deleting {} removed states: {}",
                    toDelete.size(),
                    toDelete.stream().map(WorkflowState::getDisplayName).toList());
            toDelete.forEach(workflow::deleteState);
        }
    }

    private Set<WorkflowState> findStatesToDelete(Workflow workflow, ReplaceWorkflowGraphCommand cmd) {
        Set<Long> keepStateIds = cmd.stateDefinitions().stream()
                .map(StateDefinition::identifier)
                .filter(id -> id instanceof NodeIdentifier.ExistingId)
                .map(id -> ((NodeIdentifier.ExistingId) id).id())
                .collect(Collectors.toSet());

        return workflow.getActiveStates().stream()
                .filter(s -> s.getId() != null && !keepStateIds.contains(s.getId()))
                .collect(Collectors.toSet());
    }

    private void rewireExistingTransition(
            Workflow workflow,
            Long transitionId,
            WorkflowState src,
            WorkflowState trg,
            Map<Long, WorkflowTransition> existingTransitions) {
        WorkflowTransition transition = existingTransitions.get(transitionId);
        if (transition == null) {
            throw new WorkflowTransitionNotFoundException(workflow.getId(), transitionId);
        }

        workflow.rewireTransitionSource(transition, src);
        workflow.rewireTransitionTarget(transition, trg);
        log.debug(
                "Rewired transition: id={}, source={}, target={}",
                transitionId,
                src.getDisplayName(),
                trg.getDisplayName());
    }

    private Map<Long, WorkflowTransition> indexExistingTransitions(Workflow wf) {
        Map<Long, WorkflowTransition> existingTransitions = new HashMap<>();
        for (WorkflowTransition t : wf.getTransitions()) {
            if (t.getId() != null) {
                existingTransitions.put(t.getId(), t);
            }
        }
        return existingTransitions;
    }

    private void deleteRemovedTransitions(Workflow workflow, List<TransitionDefinition> transitionDefinitions) {
        Set<Long> reqIds = transitionDefinitions.stream()
                .map(TransitionDefinition::identifier)
                .filter(id -> id instanceof NodeIdentifier.ExistingId)
                .map(id -> ((NodeIdentifier.ExistingId) id).id())
                .collect(Collectors.toSet());

        for (WorkflowTransition t : List.copyOf(workflow.getTransitions())) {
            if (t.getId() != null && !reqIds.contains(t.getId())) {
                log.debug("Deleting removed transition: id={}, name={}", t.getId(), t.getDisplayName());
                workflow.deleteTransition(t);
            }
        }
    }

    private void validateNewStateFields(StateDefinition s) {
        if (s.name() == null || s.color() == null) {
            throw new BadRequestException(INCOMPLETE_NEW_STATE);
        }
    }

    private void validateNewTransitionFields(TransitionDefinition t) {
        if (t.name() == null) {
            throw new BadRequestException(INCOMPLETE_NEW_TRANSITION);
        }
    }

    private record StateResolver(Map<Long, WorkflowState> existingStates, Map<String, WorkflowState> newStates) {
        WorkflowState resolve(NodeIdentifier ref) {
            return switch (ref) {
                case NodeIdentifier.ExistingId(Long id) -> resolveExisting(id);
                case NodeIdentifier.TempKey(String key) -> resolveNew(key);
            };
        }

        private WorkflowState resolveExisting(Long id) {
            return Optional.ofNullable(existingStates.get(id))
                    .orElseThrow(() -> new IllegalArgumentException("Invalid workflow state id: %d".formatted(id)));
        }

        private WorkflowState resolveNew(String tempKey) {
            return Optional.ofNullable(newStates.get(tempKey))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid workflow state temporary key: %s.".formatted(tempKey)));
        }
    }
}
