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

import com.tissue.api.common.exception.type.BadRequestException;
import com.tissue.api.common.vo.Label;
import com.tissue.api.workflow.application.dto.ReplaceWorkflowGraphCommand;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.domain.model.WorkflowState;
import com.tissue.api.workflow.domain.model.WorkflowTransition;
import com.tissue.api.workflow.domain.service.EntityRef;
import com.tissue.api.workflow.domain.service.WorkflowGraphValidator;
import com.tissue.api.workflow.presentation.dto.response.WorkflowResponse;
import com.tissue.api.workspace.application.service.command.WorkspaceFinder;
import com.tissue.api.workspace.domain.model.Workspace;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowGraphReplaceService {

	private final WorkspaceFinder workspaceFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowGraphValidator graphValidator;

	private record StatusResolver(
		Map<Long, WorkflowState> existingStatuses,
		Map<String, WorkflowState> newStatuses
	) {
		WorkflowState resolve(EntityRef ref) {
			return ref.isExisting()
				? resolveExisting(ref.id())
				: resolveNew(ref.tempKey());
		}

		private WorkflowState resolveExisting(Long id) {
			return Optional.ofNullable(existingStatuses.get(id))
				.orElseThrow(() -> new BadRequestException("Unknown status id: " + id));
		}

		private WorkflowState resolveNew(String tempKey) {
			return Optional.ofNullable(newStatuses.get(tempKey))
				.orElseThrow(() -> new BadRequestException("Unknown status tempKey: " + tempKey));
		}
	}

	@Transactional
	public WorkflowResponse replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {
		Workflow wf = loadWorkflowAndCheckVersion(cmd);

		graphValidator.validateWorkflowGraphStructure(
			cmd.stateCommands().stream().map(s -> s.toValidationData()).toList(),
			cmd.transitionCommands().stream().map(t -> t.toValidationData()).toList()
		);

		StatusResolver statusResolver = buildStatusResolver(wf, cmd.stateCommands());
		syncTransitions(wf, cmd.transitionCommands(), statusResolver);
		applyTerminalFlagChanges(wf, cmd.stateCommands(), statusResolver);
		WorkflowState initial = resolveAndApplyInitial(wf, cmd.stateCommands(), statusResolver);

		graphValidator.ensureValidWorkflowGraph(wf);

		deleteRemovedStatuses(cmd, wf, initial);

		return WorkflowResponse.from(wf);
	}

	private void deleteRemovedStatuses(
		ReplaceWorkflowGraphCommand cmd,
		Workflow wf,
		WorkflowState initial
	) {
		Set<WorkflowState> toDelete = findStatusesToDelete(wf, cmd.stateCommands());

		// ensureNotDeletingStatusesInUse(toDelete);
		graphValidator.ensureNotDeletingInitial(toDelete, initial);

		toDelete.forEach(wf::softDeleteState);
	}

	// TODO: 요구사항이 생기면 이슈에 대한 WorkflowStatus 마이그레이션 제공
	// private void ensureNotDeletingStatusesInUse(
	// 	Set<WorkflowStatus> toDelete
	// ) {
	// 	for (WorkflowStatus status : toDelete) {
	// 		boolean hasIssues = issueRepository.existsByWorkflowStatus(status);
	// 		if (hasIssues) {
	// 			throw new InvalidOperationException(
	// 				"Cannot delete status '" + status.getLabel() +
	// 					"' because it is currently in use by one or more issues. " +
	// 					"Please move all issues to another status before deleting.");
	// 		}
	// 	}
	// }

	private Set<WorkflowState> findStatusesToDelete(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands
	) {
		Set<Long> keepIds = stateCommands.stream()
			.map(cmd -> cmd.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		return wf.getStates().stream()
			.filter(s -> !s.isArchived())
			.filter(s -> s.getId() != null && !keepIds.contains(s.getId()))
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Workflow loadWorkflowAndCheckVersion(ReplaceWorkflowGraphCommand cmd) {
		Workspace ws = workspaceFinder.findWorkspace(cmd.workspaceKey());
		Workflow wf = workflowFinder.findWorkflow(ws, cmd.workflowId());

		if (!Objects.equals(wf.getVersion(), cmd.version())) {
			throw new IllegalStateException("Version mismatch");
		}
		return wf;
	}

	private WorkflowGraphReplaceService.StatusResolver buildStatusResolver(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands
	) {
		Map<Long, WorkflowState> existingStatuses = new HashMap<>();
		Map<String, WorkflowState> newStatuses = new HashMap<>();

		for (WorkflowState s : wf.getStates()) {
			existingStatuses.put(s.getId(), s);
		}

		for (var s : stateCommands) {
			if (s.ref().isExisting()) {
				continue;
			}
			WorkflowState created = wf.addState(
				Label.of(s.label()),
				s.description(),
				s.color(),
				s.initial(),
				s.terminal()
			);
			newStatuses.put(s.ref().tempKey(), created);
		}

		return new StatusResolver(existingStatuses, newStatuses);
	}

	private void syncTransitions(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.TransitionCommand> transitionCommands,
		StatusResolver statusResolver
	) {
		deleteRemovedTransitions(wf, transitionCommands);
		Map<Long, WorkflowTransition> existingTransitions = indexExistingTransitions(wf);

		for (var cmd : transitionCommands) {
			WorkflowState src = statusResolver.resolve(cmd.source());
			WorkflowState trg = statusResolver.resolve(cmd.target());

			if (cmd.ref().isExisting()) {
				rewireExistingTransition(wf, cmd, src, trg, existingTransitions);
				continue;
			}

			addNewTransition(wf, cmd, src, trg);
		}
	}

	private void rewireExistingTransition(
		Workflow wf,
		ReplaceWorkflowGraphCommand.TransitionCommand cmd,
		WorkflowState src,
		WorkflowState trg,
		Map<Long, WorkflowTransition> existingTransitions
	) {
		WorkflowTransition transition = existingTransitions.get(cmd.ref().id());
		if (transition == null) {
			throw new BadRequestException("Unknown transition id: " + cmd.ref().id());
		}
		wf.rewireTransitionSource(transition, src);
		wf.rewireTransitionTarget(transition, trg);
	}

	private void addNewTransition(
		Workflow wf,
		ReplaceWorkflowGraphCommand.TransitionCommand cmd,
		WorkflowState src,
		WorkflowState trg
	) {
		wf.addTransition(Label.of(cmd.label()), cmd.description(), src, trg);
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
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.TransitionCommand> transitionCommands
	) {
		Set<Long> reqIds = transitionCommands.stream()
			.map(t -> t.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		for (WorkflowTransition t : List.copyOf(wf.getTransitions())) {
			if (t.getId() != null && !reqIds.contains(t.getId())) {
				wf.softDeleteTransition(t);
			}
		}
	}

	private void applyTerminalFlagChanges(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands,
		StatusResolver statusResolver
	) {
		for (var cmd : stateCommands) {
			boolean isNewStatus = !cmd.ref().isExisting();
			if (isNewStatus) {
				continue;
			}

			WorkflowState status = statusResolver.resolve(cmd.ref());
			wf.updateStateTerminalFlag(status, cmd.terminal());
		}
	}

	private WorkflowState resolveAndApplyInitial(
		Workflow wf,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands,
		StatusResolver statusResolver
	) {
		var cmd = stateCommands.stream()
			.filter(ReplaceWorkflowGraphCommand.StateCommand::initial)
			.findFirst()
			.orElseThrow(() -> new BadRequestException("Initial not provided"));

		WorkflowState requested = statusResolver.resolve(cmd.ref());

		if (requested != wf.getInitialState()) {
			wf.updateInitialState(requested);
		}
		return requested;
	}
}
