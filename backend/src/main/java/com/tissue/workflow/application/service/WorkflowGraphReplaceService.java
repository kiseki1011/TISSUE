package com.tissue.workflow.application.service;

import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.dto.EntityRef;
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
import com.tissue.workflow.domain.exception.WorkflowExceptions;
import jakarta.persistence.OptimisticLockException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkflowGraphReplaceService implements WorkflowGraphReplaceUseCase {

    private final ProjectFinder projectFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowGraphValidator graphValidator;
    private final WorkflowValidator workflowValidator;

    @Override
    public void replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {
        Workflow workflow = loadWorkflowAndCheckVersion(cmd);

        StateResolver stateResolver = buildStateResolver(workflow, cmd.stateDefinitions());

        syncTransitions(workflow, cmd.transitionDefinitions(), stateResolver);

        applyStateCategoryChanges(workflow, cmd.stateDefinitions(), stateResolver);

        resolveAndSetInitial(workflow, cmd.stateDefinitions(), stateResolver);

        deleteRemovedStates(workflow, cmd);

        graphValidator.ensureValidWorkflowGraph(workflow);
    }

    private Workflow loadWorkflowAndCheckVersion(ReplaceWorkflowGraphCommand cmd) {
        Project project = projectFinder.getModifiableBy(cmd.projectKey(), cmd.workspaceKey());
        Workflow workflow = workflowFinder.findBy(cmd.workflowId(), project);

        if (!Objects.equals(workflow.getVersion(), cmd.version())) {
            throw new OptimisticLockException(
                    ("Workflow version mismatch. Workflow version from client was '%d', while"
                                    + " current version is '%d'.")
                            .formatted(cmd.version(), workflow.getVersion()));
        }
        return workflow;
    }

    private StateResolver buildStateResolver(
            Workflow workflow, List<StateDefinition> stateDefinitions) {
        Map<Long, WorkflowState> existingStatuses = new HashMap<>();
        Map<String, WorkflowState> newStatuses = new HashMap<>();

        for (WorkflowState s : workflow.getStates()) {
            existingStatuses.put(s.getId(), s);
        }

        for (var s : stateDefinitions) {
            if (s.stateRef().isExisting()) {
                continue;
            }
            WorkflowState created =
                    workflow.addState(s.name(), s.description(), s.color(), s.category());
            newStatuses.put(s.stateRef().tempKey(), created);
        }

        return new StateResolver(existingStatuses, newStatuses);
    }

    private void syncTransitions(
            Workflow workflow,
            List<TransitionDefinition> transitionDefinitions,
            StateResolver stateResolver) {
        deleteRemovedTransitions(workflow, transitionDefinitions);
        Map<Long, WorkflowTransition> existingTransitions = indexExistingTransitions(workflow);

        for (var cmd : transitionDefinitions) {
            WorkflowState src = stateResolver.resolve(cmd.sourceStateRef());
            WorkflowState trg = stateResolver.resolve(cmd.targetStateRef());

            if (cmd.transitionRef().isExisting()) {
                rewireExistingTransition(workflow, cmd, src, trg, existingTransitions);
                continue;
            }

            workflow.addTransition(cmd.name(), cmd.description(), src, trg);
        }
    }

    private void applyStateCategoryChanges(
            Workflow workflow, List<StateDefinition> stateDefinitions, StateResolver resolver) {
        for (var cmd : stateDefinitions) {
            boolean stateReferenceNotExist = !cmd.stateRef().isExisting();
            if (stateReferenceNotExist) {
                continue;
            }
            WorkflowState state = resolver.resolve(cmd.stateRef());
            workflow.changeStateCategory(state, cmd.category());
        }
    }

    private void resolveAndSetInitial(
            Workflow workflow,
            List<StateDefinition> stateDefinitions,
            StateResolver stateResolver) {
        var todoCmds =
                stateDefinitions.stream()
                        .filter(cmd -> cmd.category() == StateCategory.INITIAL)
                        .toList();

        if (todoCmds.size() != 1) {
            throw WorkflowExceptions.invalidInitialStateCount(todoCmds.size());
        }

        WorkflowState todoState = stateResolver.resolve(todoCmds.get(0).stateRef());

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

    private Set<WorkflowState> findStatesToDelete(
            Workflow workflow, ReplaceWorkflowGraphCommand cmd) {
        Set<Long> keepStateIds =
                cmd.stateDefinitions().stream()
                        .map(s -> s.stateRef().id())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        return workflow.getActiveStates().stream()
                .filter(s -> s.getId() != null && !keepStateIds.contains(s.getId()))
                .collect(Collectors.toSet());
    }

    private void rewireExistingTransition(
            Workflow workflow,
            TransitionDefinition cmd,
            WorkflowState src,
            WorkflowState trg,
            Map<Long, WorkflowTransition> existingTransitions) {
        WorkflowTransition transition = existingTransitions.get(cmd.transitionRef().id());
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

    private void deleteRemovedTransitions(
            Workflow workflow, List<TransitionDefinition> transitionDefinitions) {
        Set<Long> reqIds =
                transitionDefinitions.stream()
                        .map(t -> t.transitionRef().id())
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        for (WorkflowTransition t : List.copyOf(workflow.getTransitions())) {
            if (t.getId() != null && !reqIds.contains(t.getId())) {
                workflow.deleteTransition(t);
            }
        }
    }

    private record StateResolver(
            Map<Long, WorkflowState> existingStates, Map<String, WorkflowState> newStates) {
        WorkflowState resolve(EntityRef ref) {
            return ref.isExisting() ? resolveExisting(ref.id()) : resolveNew(ref.tempKey());
        }

        private WorkflowState resolveExisting(Long id) {
            return Optional.ofNullable(existingStates.get(id))
                    .orElseThrow(
                            () ->
                                    new IllegalArgumentException(
                                            "Invalid workflow state id '%d'".formatted(id)));
        }

        private WorkflowState resolveNew(String tempKey) {
            return Optional.ofNullable(newStates.get(tempKey))
                    .orElseThrow(
                            () ->
                                    new IllegalArgumentException(
                                            "Invalid workflow state temporary key '%s'."
                                                    .formatted(tempKey)));
        }
    }
}
