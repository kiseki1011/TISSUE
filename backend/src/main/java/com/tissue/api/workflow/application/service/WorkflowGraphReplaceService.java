package com.tissue.api.workflow.application.service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.application.dto.ReplaceWorkflowGraphCommand;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.service.EntityRef;
import com.tissue.api.workflow.domain.service.WorkflowGraphValidator;
import com.tissue.api.workflow.presentation.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowGraphReplaceService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowGraphValidator graphValidator;

	private record StateResolver(
		Map<Long, WorkflowState> existingStates,
		Map<String, WorkflowState> newStates
	) {
		WorkflowState resolve(EntityRef ref) {
			return ref.isExisting()
				? resolveExisting(ref.id())
				: resolveNew(ref.tempKey());
		}

		private WorkflowState resolveExisting(Long id) {
			return Optional.ofNullable(existingStates.get(id))
				.orElseThrow(() -> new IllegalArgumentException("Invalid workflow state id '%d'."
					.formatted(id)));
		}

		private WorkflowState resolveNew(String tempKey) {
			return Optional.ofNullable(newStates.get(tempKey))
				.orElseThrow(() -> new IllegalArgumentException("Invalid workflow state temporary key '%s'."
					.formatted(tempKey)));
		}
	}

	@Transactional
	public WorkflowResponse replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {
		Workflow workflow = loadWorkflowAndCheckVersion(cmd);

		graphValidator.validateWorkflowGraphStructure(
			cmd.stateCommands().stream().map(s -> s.toValidationData()).toList(),
			cmd.transitionCommands().stream().map(t -> t.toValidationData()).toList()
		);

		StateResolver stateResolver = buildStateResolver(workflow, cmd.stateCommands());
		syncTransitions(workflow, cmd.transitionCommands(), stateResolver);
		applyTerminalFlagChanges(workflow, cmd.stateCommands(), stateResolver);
		WorkflowState initial = resolveAndApplyInitial(workflow, cmd.stateCommands(), stateResolver);

		graphValidator.ensureValidWorkflowGraph(workflow);

		deleteRemovedStatuses(cmd.stateCommands(), workflow, initial);

		return WorkflowResponse.from(workflow);
	}

	private void deleteRemovedStatuses(
		List<ReplaceWorkflowGraphCommand.StateCommand> states,
		Workflow workflow,
		WorkflowState initial
	) {
		Set<WorkflowState> toDelete = findStatesToDelete(workflow, states);

		// TODO: 아래 두 메서드를 ensureStateDeletable()로 묶기?
		// graphValidator.ensureNotDeletingStatusesInUse(toDelete);
		graphValidator.ensureNotDeletingInitial(toDelete, initial);

		toDelete.forEach(workflow::softDeleteState);
	}

	// TODO: 현재 내 삭제 정책은 괜찮나?
	// TODO: WorkflowGraphValidator로 이동
	// private void ensureNotDeletingStatesInUse(
	// 	Set<WorkflowState> toDelete
	// ) {
	// 	for (WorkflowState state : toDelete) {
	// 		boolean hasIssues = issueRepository.existsByWorkflowState(state);
	// 		if (hasIssues) {
	// 			throw new WorkflowStateInUseNotDeletableException(
	// 				"Cannot delete workflow state '" + state.getLabel() +
	// 					"' because it is currently in use by one or more issues.");
	// 		}
	// 	}
	// }

	private Set<WorkflowState> findStatesToDelete(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.StateCommand> states
	) {
		Set<Long> keepIds = states.stream()
			.map(cmd -> cmd.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		return workflow.getStates().stream()
			.filter(s -> !s.isArchived())
			.filter(s -> s.getId() != null && !keepIds.contains(s.getId()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Workflow loadWorkflowAndCheckVersion(ReplaceWorkflowGraphCommand cmd) {
		Workspace workspace = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow workflow = workflowFinder.findWorkflow(workspace, cmd.workflowId());

		if (!Objects.equals(workflow.getVersion(), cmd.version())) {
			throw new OptimisticLockException(("Workflow version mismatch. "
				+ "Workflow version from client was '%d', while current version is '%d'.")
				.formatted(cmd.version(), workflow.getVersion()));
		}
		return workflow;
	}

	private StateResolver buildStateResolver(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands
	) {
		Map<Long, WorkflowState> existingStatuses = new HashMap<>();
		Map<String, WorkflowState> newStatuses = new HashMap<>();

		for (WorkflowState s : workflow.getStates()) {
			existingStatuses.put(s.getId(), s);
		}

		for (var s : stateCommands) {
			if (s.ref().isExisting()) {
				continue;
			}
			WorkflowState created = workflow.addState(
				Label.of(s.label()),
				s.description(),
				s.color(),
				s.initial(),
				s.terminal()
			);
			newStatuses.put(s.ref().tempKey(), created);
		}

		return new StateResolver(existingStatuses, newStatuses);
	}

	private void syncTransitions(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.TransitionCommand> transitionCommands,
		StateResolver stateResolver
	) {
		deleteRemovedTransitions(workflow, transitionCommands);
		Map<Long, WorkflowTransition> existingTransitions = indexExistingTransitions(workflow);

		for (var cmd : transitionCommands) {
			WorkflowState src = stateResolver.resolve(cmd.source());
			WorkflowState trg = stateResolver.resolve(cmd.target());

			if (cmd.ref().isExisting()) {
				rewireExistingTransition(workflow, cmd, src, trg, existingTransitions);
				continue;
			}

			addNewTransition(workflow, cmd, src, trg);
		}
	}

	private void rewireExistingTransition(
		Workflow workflow,
		ReplaceWorkflowGraphCommand.TransitionCommand cmd,
		WorkflowState src,
		WorkflowState trg,
		Map<Long, WorkflowTransition> existingTransitions
	) {
		WorkflowTransition transition = existingTransitions.get(cmd.ref().id());
		if (transition == null) {
			throw new IllegalArgumentException("Invalid workflow transition id '%d'.".formatted(cmd.ref().id()));
		}
		workflow.rewireTransitionSource(transition, src);
		workflow.rewireTransitionTarget(transition, trg);
	}

	private void addNewTransition(
		Workflow workflow,
		ReplaceWorkflowGraphCommand.TransitionCommand cmd,
		WorkflowState src,
		WorkflowState trg
	) {
		workflow.addTransition(Label.of(cmd.label()), cmd.description(), src, trg);
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
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.TransitionCommand> transitionCommands
	) {
		Set<Long> reqIds = transitionCommands.stream()
			.map(t -> t.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		for (WorkflowTransition t : List.copyOf(workflow.getTransitions())) {
			if (t.getId() != null && !reqIds.contains(t.getId())) {
				workflow.softDeleteTransition(t);
			}
		}
	}

	private void applyTerminalFlagChanges(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands,
		StateResolver stateResolver
	) {
		for (var cmd : stateCommands) {
			boolean isNewStatus = !cmd.ref().isExisting();
			if (isNewStatus) {
				continue;
			}

			WorkflowState status = stateResolver.resolve(cmd.ref());
			workflow.updateStateTerminalFlag(status, cmd.terminal());
		}
	}

	private WorkflowState resolveAndApplyInitial(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands,
		StateResolver stateResolver
	) {
		var cmd = stateCommands.stream()
			.filter(ReplaceWorkflowGraphCommand.StateCommand::initial)
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Initial workflow state must be provided."));

		WorkflowState requested = stateResolver.resolve(cmd.ref());

		if (requested != workflow.getInitialState()) {
			workflow.updateInitialState(requested);
		}
		return requested;
	}
}
