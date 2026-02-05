package com.tissue.workflow.application.service;

import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.workflow.application.dto.NodeIdentifier;
import com.tissue.workflow.application.dto.StateDefinition;
import com.tissue.workflow.application.dto.TransitionDefinition;
import com.tissue.workflow.application.dto.request.ReplaceWorkflowGraphCommand;
import com.tissue.workflow.application.port.in.WorkflowGraphReplaceUseCase;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.workflow.application.service.validator.WorkflowValidator;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workflow.domain.exception.InvalidInitialStateCountException;
import com.tissue.workflow.domain.exception.WorkflowTransitionNotFoundException;
import com.tissue.workflow.domain.exception.WorkflowVersionMismatchException;
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

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkflowGraphReplaceService implements WorkflowGraphReplaceUseCase {

    private final WorkflowFinder workflowFinder;
    private final WorkflowGraphValidator graphValidator;
    private final WorkflowValidator workflowValidator;
    private final ProjectAuthorizationService projectAuthService;

    // TODO: add javadoc to explain process(consider adding javadoc to private methods for complex processes)
    // TODO: add logging(inlcuding debug logging, this method needs thorough testing)
    @Override
    public void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        Workflow workflow = workflowFinder.getWithProjectBy(
                actorContext.workspaceKey(), actorContext.projectKey(), cmd.workflowId());

        projectAuthService.requireWorkflowEditPermission(actorContext, workflow);

        checkWorkflowVersion(cmd, workflow);

        StateResolver stateResolver = buildStateResolver(workflow, cmd.stateDefinitions());

        syncTransitions(workflow, cmd.transitionDefinitions(), stateResolver);

        applyStateCategoryChanges(workflow, cmd.stateDefinitions(), stateResolver);

        resolveAndSetInitial(workflow, cmd.stateDefinitions(), stateResolver);

        deleteRemovedStates(workflow, cmd);

        graphValidator.ensureValidWorkflowGraph(workflow);
    }

    private void checkWorkflowVersion(ReplaceWorkflowGraphCommand cmd, Workflow workflow) {
        if (!Objects.equals(workflow.getVersion(), cmd.version())) {
            throw new WorkflowVersionMismatchException(cmd.version(), workflow.getVersion());
        }
    }

    private StateResolver buildStateResolver(Workflow workflow, List<StateDefinition> stateDefinitions) {
        Map<Long, WorkflowState> existingStatuses = new HashMap<>();
        Map<String, WorkflowState> newStatuses = new HashMap<>();

        for (WorkflowState s : workflow.getStates()) {
            existingStatuses.put(s.getId(), s);
        }

        for (var s : stateDefinitions) {
            if (s.identifier() instanceof NodeIdentifier.TempKey(String key)) {
                WorkflowState created = workflow.addState(s.name(), s.description(), s.color(), s.category());
                newStatuses.put(key, created);
            }
        }

        return new StateResolver(existingStatuses, newStatuses);
    }

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

            workflow.addTransition(cmd.name(), cmd.description(), src, trg);
        }
    }

    private void applyStateCategoryChanges(
            Workflow workflow, List<StateDefinition> stateDefinitions, StateResolver resolver) {

        for (var cmd : stateDefinitions) {
            if (cmd.identifier() instanceof NodeIdentifier.ExistingId) {
                WorkflowState state = resolver.resolve(cmd.identifier());
                workflow.changeStateCategory(state, cmd.category());
            }
        }
    }

    private void resolveAndSetInitial(
            Workflow workflow, List<StateDefinition> stateDefinitions, StateResolver stateResolver) {
        var todoCmds = stateDefinitions.stream()
                .filter(cmd -> cmd.category() == StateCategory.INITIAL)
                .toList();

        if (todoCmds.size() != 1) {
            throw new InvalidInitialStateCountException(todoCmds.size());
        }

        WorkflowState todoState = stateResolver.resolve(todoCmds.getFirst().identifier());

        workflow.setInitialState(todoState);
    }

    private void deleteRemovedStates(Workflow workflow, ReplaceWorkflowGraphCommand cmd) {
        Set<WorkflowState> toDelete = findStatesToDelete(workflow, cmd);

        boolean toDeleteExist = !toDelete.isEmpty();
        if (toDeleteExist) {
            workflowValidator.ensureStatesDeletable(toDelete);
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
            throw new WorkflowTransitionNotFoundException(workflow.getProjectKey(), workflow.getId(), transitionId);
        }
        workflow.rewireTransitionSource(transition, src);
        workflow.rewireTransitionTarget(transition, trg);
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
                workflow.deleteTransition(t);
            }
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
