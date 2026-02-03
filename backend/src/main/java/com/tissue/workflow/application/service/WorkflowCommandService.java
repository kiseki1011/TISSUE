package com.tissue.workflow.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.project.application.dto.ProjectMemberContext;
import com.tissue.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.project.application.service.finder.ProjectFinder;
import com.tissue.project.domain.Project;
import com.tissue.workflow.application.dto.NodeIdentifier;
import com.tissue.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.workflow.application.dto.request.DeleteWorkflowCommand;
import com.tissue.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.workflow.application.port.in.WorkflowCommandUseCase;
import com.tissue.workflow.application.port.out.WorkflowRepository;
import com.tissue.workflow.application.service.finder.WorkflowFinder;
import com.tissue.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.workflow.application.service.validator.WorkflowValidator;
import com.tissue.workflow.domain.Workflow;
import com.tissue.workflow.domain.WorkflowState;
import com.tissue.workflow.domain.WorkflowTransition;
import com.tissue.workflow.domain.exception.DuplicateWorkflowNameException;
import com.tissue.workflow.domain.exception.InvalidGraphRequestException;
import com.tissue.workflow.domain.exception.WorkflowTransitionNotFoundException;
import com.tissue.workflow.domain.guard.GuardType;
import com.tissue.workflow.domain.guard.TransitionGuard;
import com.tissue.workflow.domain.service.TransitionGuardRegistry;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkflowCommandService implements WorkflowCommandUseCase {

    private final ProjectFinder projectFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowRepository workflowRepository;
    private final WorkflowValidator workflowValidator;
    private final WorkflowGraphValidator graphValidator;
    private final TransitionGuardRegistry guardRegistry;
    private final ProjectAuthorizationService projectAuthService;

    @Override
    public WorkflowCreateResponse create(CreateWorkflowCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();

        // TODO: requireWorkspaceAdmin or requireProjectCreator -> requireWorkflowCreatePermission

        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        workflowValidator.ensureNameUnique(project, cmd.name());

        try {
            Workflow workflow =
                    workflowRepository.save(Workflow.create(project, cmd.name(), cmd.description(), cmd.color()));

            Map<String, WorkflowState> stateByTempKey = new HashMap<>();

            for (var s : cmd.stateDefinitions()) {
                WorkflowState state = workflow.addState(s.name(), s.description(), s.color(), s.category());

                if (s.identifier() instanceof NodeIdentifier.TempKey(String key)) {
                    stateByTempKey.put(key, state);
                } else {
                    throw new InvalidGraphRequestException(
                            "Creation requires temporary keys", "state", "invalid_identifier_type");
                }
            }

            for (var t : cmd.transitionDefinitions()) {
                String sourceKey = ((NodeIdentifier.TempKey) t.sourceIdentifier()).key();
                String targetKey = ((NodeIdentifier.TempKey) t.targetIdentifier()).key();

                WorkflowState source = stateByTempKey.get(sourceKey);
                WorkflowState target = stateByTempKey.get(targetKey);

                if (source == null) {
                    throw new InvalidGraphRequestException(
                            "Source state not found for key: " + sourceKey, "transition", "missing_source_state");
                }
                if (target == null) {
                    throw new InvalidGraphRequestException(
                            "Target state not found for key: " + targetKey, "transition", "missing_target_state");
                }

                workflow.addTransition(t.name(), t.description(), source, target);
            }

            graphValidator.ensureValidWorkflowGraph(workflow);

            return WorkflowCreateResponse.from(workflow);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateWorkflowNameException(
                    cmd.name().getDisplay(), project.getKey(), project.getWorkspaceKey());
        }
    }

    @Override
    public void update(UpdateWorkflowCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Workflow workflow = workflowFinder.getBy(cmd.workflowId(), project);

        projectAuthService.requireWorkflowEditPermission(actorContext, workflow);

        Patchers.apply(cmd.name(), newName -> {
            if (!workflow.getName().equals(newName)) {
                workflowValidator.ensureNameUnique(project, newName);
                workflow.rename(newName);
            }
        });
        Patchers.apply(cmd.description(), workflow::updateDescription);
        Patchers.apply(cmd.color(), workflow::updateColor);
    }

    @Override
    public void delete(DeleteWorkflowCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Workflow workflow = workflowFinder.getBy(cmd.workflowId(), project);

        projectAuthService.requireWorkflowEditPermission(actorContext, workflow);

        // TODO: archive(soft-delete) 정책 정하기
        //  - 정책1: 해당 워크플로우를 사용하는 이슈가 단 하나라도 존재한다면 불가
        //  - 정책2: 해당 워크플로우를 사용하는 이슈가 있더라도, 전부 category가 DONE이라면 허용
        //    UI에서 해당 DONE 상태의 이슈들의 state는 회색으로 변경(disable 되었다는 표시)
        // workflowValidator.ensureDeletable();

        workflow.softDelete();

        // TODO: WorkflowDeletedEvent (Do i really need this?)
    }

    // TODO: is there a better name? updateStateData? updateStateMetaData?
    @Override
    public void updateState(UpdateStateCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Workflow workflow = workflowFinder.getBy(cmd.workflowId(), project);
        WorkflowState state = workflowFinder.getStateBy(cmd.stateId(), workflow);

        projectAuthService.requireWorkflowEditPermission(actorContext, workflow);

        Patchers.apply(cmd.name(), l -> workflow.renameState(state, l));
        Patchers.apply(cmd.description(), state::updateDescription);
        Patchers.apply(cmd.color(), state::updateColor);
    }

    // TODO: is there a better name? updateTransitionData? updateTransitionMetaData?
    @Override
    public void updateTransition(UpdateTransitionCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Workflow workflow = workflowFinder.getBy(cmd.workflowId(), project);
        WorkflowTransition transition = workflowFinder.getTransitionBy(cmd.transitionId(), workflow);

        projectAuthService.requireWorkflowEditPermission(actorContext, workflow);

        Patchers.apply(cmd.name(), l -> workflow.renameTransition(transition, l));
        Patchers.apply(cmd.description(), transition::updateDescription);
    }

    // TODO: add javadoc to explain the process
    @Override
    public void configureTransitionGuards(ConfigureTransitionGuardsCommand cmd) {
        ProjectMemberContext actorContext = cmd.actorContext();
        Project project = projectFinder.getModifiableBy(actorContext.projectId());
        Workflow workflow = workflowFinder.getBy(cmd.workflowId(), project);

        projectAuthService.requireWorkflowEditPermission(actorContext, workflow);

        WorkflowTransition transition = workflow.getTransitions().stream()
                .filter(t -> t.getId().equals(cmd.transitionId()))
                .findFirst()
                .orElseThrow(() -> new WorkflowTransitionNotFoundException(cmd.transitionId(), workflow.getId()));

        workflow.clearGuardsForTransition(transition);

        Set<GuardType> usedTypes = new HashSet<>();

        for (var g : cmd.guards()) {
            guardRegistry.ensureGuardExists(g.guardType());
            workflowValidator.ensureNoDuplicateGuard(g, usedTypes);

            Map<String, Object> params = g.params() != null ? g.params() : Collections.emptyMap();

            TransitionGuard guardImplementation = guardRegistry.getGuard(g.guardType());
            guardImplementation.validateParams(params, g.guardType());

            workflow.addTransitionGuard(transition, g.guardType(), params, g.order());
        }
    }
}
