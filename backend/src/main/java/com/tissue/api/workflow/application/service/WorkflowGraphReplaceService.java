package com.tissue.api.workflow.application.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.vo.Label;
import com.tissue.api.issue.domain.enums.StateCategory;
import com.tissue.api.project.application.service.finder.ProjectFinder;
import com.tissue.api.project.domain.Project;
import com.tissue.api.workflow.application.dto.ReplaceWorkflowGraphCommand;
import com.tissue.api.workflow.application.service.finder.WorkflowFinder;
import com.tissue.api.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.service.EntityRef;
import com.tissue.api.workflow.domain.service.WorkflowValidator;
import com.tissue.api.workflow.presentation.dto.response.WorkflowResponse;

import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorkflowGraphReplaceService {

	private final ProjectFinder projectFinder;
	private final WorkflowFinder workflowFinder;
	private final WorkflowGraphValidator graphValidator;
	private final WorkflowValidator workflowValidator;

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
					.formatted(id))
				);
		}

		private WorkflowState resolveNew(String tempKey) {
			return Optional.ofNullable(newStates.get(tempKey))
				.orElseThrow(() -> new IllegalArgumentException("Invalid workflow state temporary key '%s'."
					.formatted(tempKey))
				);
		}
	}

	@Transactional
	public WorkflowResponse replaceWorkflowGraph(ReplaceWorkflowGraphCommand cmd) {
		Workflow workflow = loadWorkflowAndCheckVersion(cmd);

		StateResolver stateResolver = buildStateResolver(workflow, cmd.stateCommands());

		syncTransitions(workflow, cmd.transitionCommands(), stateResolver);

		applyStateAttributesUpdates(workflow, cmd.stateCommands(), stateResolver);

		resolveAndSetInitial(workflow, cmd.stateCommands(), stateResolver);

		deleteRemovedStates(workflow, cmd);

		graphValidator.ensureValidWorkflowGraph(workflow);

		return WorkflowResponse.from(workflow);
	}

	private Workflow loadWorkflowAndCheckVersion(ReplaceWorkflowGraphCommand cmd) {
		Project project = projectFinder.findBy(cmd.projectKey(), cmd.workspaceKey());
		Workflow workflow = workflowFinder.findBy(project, cmd.workflowId());

		if (!Objects.equals(workflow.getVersion(), cmd.version())) {
			// TODO: 메세지 경량화 고려
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
				s.category()
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

	private void applyStateAttributesUpdates(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.StateCommand> cmds,
		StateResolver resolver
	) {
		for (var cmd : cmds) {
			// 신규 생성된 건 이미 addState 할 때 값이 들어갔으므로 패스
			if (!cmd.ref().isExisting()) {
				continue;
			}

			WorkflowState state = resolver.resolve(cmd.ref());

			// 기본 속성 업데이트
			// TODO: 기본 속성 변경은 실시간으로 반영되도록 따로 분리 고려
			workflow.renameState(state, Label.of(cmd.label()));
			state.updateDescription(cmd.description());
			state.updateColor(cmd.color());

			// TODO <-> IN_PROGRESS 변경 등이 여기서 일어남.
			// 일시적으로 TODO가 0개나 2개가 될 수 있지만, 트랜잭션 마지막에 Validator가 잡아줌.
			workflow.changeStateCategory(state, cmd.category());
		}
	}

	private void resolveAndSetInitial(
		Workflow workflow,
		List<ReplaceWorkflowGraphCommand.StateCommand> stateCommands,
		StateResolver stateResolver
	) {
		var todoCmds = stateCommands.stream()
			.filter(cmd -> cmd.category() == StateCategory.TODO)
			.toList();

		// TODO: 굳이 여기서 fast-fail 해야 하나?
		if (todoCmds.size() != 1) {
			// TODO: InvalidWorkflowGraphException
			throw new IllegalArgumentException("Workflow must have exactly one 'TODO' state.");
		}

		WorkflowState todoState = stateResolver.resolve(todoCmds.get(0).ref());

		workflow.setInitialState(todoState);
	}

	private void deleteRemovedStates(Workflow workflow, ReplaceWorkflowGraphCommand cmd) {
		Set<WorkflowState> toDelete = findStatesToDelete(workflow, cmd);

		boolean toDeleteExist = !toDelete.isEmpty();
		if (toDeleteExist) {
			workflowValidator.ensureStatesDeletable(toDelete);
			toDelete.forEach(workflow::softDeleteState);
		}
	}

	private Set<WorkflowState> findStatesToDelete(Workflow workflow, ReplaceWorkflowGraphCommand cmd) {
		Set<Long> keepStateIds = cmd.stateCommands().stream()
			.map(s -> s.ref().id())
			.filter(Objects::nonNull)
			.collect(Collectors.toSet());

		return workflow.getActiveStates().stream()
			.filter(s -> s.getId() != null && !keepStateIds.contains(s.getId()))
			.collect(Collectors.toSet());
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
				workflow.deleteTransition(t);
			}
		}
	}
}
