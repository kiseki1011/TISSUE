package com.tissue.feature.workflow.application.service;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INVALID_GRAPH_REQUEST;

import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workflow.application.dto.NodeIdentifier;
import com.tissue.feature.workflow.application.dto.request.ConfigureTransitionGuardsCommand;
import com.tissue.feature.workflow.application.dto.request.CreateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateStateCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateTransitionCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowCommand;
import com.tissue.feature.workflow.application.dto.request.UpdateWorkflowVcsSettingsCommand;
import com.tissue.feature.workflow.application.dto.response.WorkflowCreateResponse;
import com.tissue.feature.workflow.application.port.repository.WorkflowRepository;
import com.tissue.feature.workflow.application.port.usecase.WorkflowCommandUseCase;
import com.tissue.feature.workflow.application.service.finder.WorkflowFinder;
import com.tissue.feature.workflow.application.service.validator.WorkflowGraphValidator;
import com.tissue.feature.workflow.application.service.validator.WorkflowValidator;
import com.tissue.feature.workflow.domain.VcsAutomationSettings;
import com.tissue.feature.workflow.domain.Workflow;
import com.tissue.feature.workflow.domain.WorkflowState;
import com.tissue.feature.workflow.domain.WorkflowTransition;
import com.tissue.feature.workflow.domain.exception.WorkflowTransitionNotFoundException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import com.tissue.feature.workflow.domain.service.TransitionGuardRegistry;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.util.Patchers;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class WorkflowCommandService implements WorkflowCommandUseCase {

    private final ProjectMemberFinder projectMemberFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowRepository workflowRepository;
    private final WorkflowValidator workflowValidator;
    private final WorkflowGraphValidator graphValidator;
    private final TransitionGuardRegistry guardRegistry;
    private final ProjectAuthorizationService projectAuthService;

    // TODO: add javadoc to explain process
    @Override
    public WorkflowCreateResponse create(
            ProjectIdentifier projectIdentifier, CreateWorkflowCommand cmd, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        Project project = actor.getProject();

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
                    throw new BadRequestException(INVALID_GRAPH_REQUEST, "Workflow creation requires temporary keys");
                }
            }

            for (var t : cmd.transitionDefinitions()) {
                String sourceKey = ((NodeIdentifier.TempKey) t.sourceIdentifier()).key();
                String targetKey = ((NodeIdentifier.TempKey) t.targetIdentifier()).key();

                WorkflowState source = stateByTempKey.get(sourceKey);
                WorkflowState target = stateByTempKey.get(targetKey);

                if (source == null) {
                    throw new BadRequestException(INVALID_GRAPH_REQUEST)
                            .addContext("reason", "Source state not found for key: " + sourceKey);
                }
                if (target == null) {
                    throw new BadRequestException(INVALID_GRAPH_REQUEST)
                            .addContext("reason", "Target state not found for key: " + targetKey);
                }

                workflow.addTransition(t.name(), t.description(), source, target);
            }

            graphValidator.ensureValidWorkflowGraph(workflow);
            return WorkflowCreateResponse.from(workflow);

        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException(DUPLICATE_WORKFLOW_NAME);
        }
    }

    @Override
    public void update(
            ProjectIdentifier projectIdentifier, Long workflowId, UpdateWorkflowCommand cmd, Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        Workflow workflow = workflowFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), workflowId);

        Patchers.apply(cmd.name(), newName -> {
            if (!Objects.equals(workflow.getName(), newName.toString())) {
                workflowValidator.ensureNameUnique(workflow.getProject(), newName);
                workflow.rename(newName);
            }
        });
        Patchers.apply(cmd.description(), workflow::updateDescription);
        Patchers.apply(cmd.color(), workflow::updateColor);
    }

    @Override
    public void delete(ProjectIdentifier projectIdentifier, Long workflowId, Long actorMemberId) {
        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        Workflow workflow = workflowFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), workflowId);

        workflowValidator.ensureWorkflowDeletable(workflow);

        workflowRepository.delete(workflow);
    }

    @Override
    public void updateState(
            ProjectIdentifier projectIdentifier,
            Long workflowId,
            Long stateId,
            UpdateStateCommand cmd,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        WorkflowState state = workflowFinder.getStateWithHierarchyBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), workflowId, stateId);

        Patchers.apply(cmd.name(), l -> state.getWorkflow().renameState(state, l));
        Patchers.apply(cmd.description(), state::updateDescription);
        Patchers.apply(cmd.color(), state::updateColor);
    }

    @Override
    public void updateTransition(
            ProjectIdentifier projectIdentifier,
            Long workflowId,
            Long transitionId,
            UpdateTransitionCommand cmd,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        WorkflowTransition transition = workflowFinder.getTransitionWithHierarchyBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), workflowId, transitionId);

        Patchers.apply(cmd.name(), l -> transition.getWorkflow().renameTransition(transition, l));
        Patchers.apply(cmd.description(), transition::updateDescription);
    }

    // TODO: add javadoc to explain the process
    @Override
    public void configureTransitionGuards(
            ProjectIdentifier projectIdentifier,
            Long workflowId,
            Long transitionId,
            ConfigureTransitionGuardsCommand cmd,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        Workflow workflow = workflowFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), workflowId);

        WorkflowTransition transition = workflow.getTransitions().stream()
                .filter(t -> t.getId().equals(transitionId))
                .findFirst()
                .orElseThrow(() -> new WorkflowTransitionNotFoundException(
                        projectIdentifier.projectKey(), workflow.getId(), transitionId));

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

    @Override
    public void updateVcsSettings(
            ProjectIdentifier projectIdentifier,
            Long workflowId,
            UpdateWorkflowVcsSettingsCommand cmd,
            Long actorMemberId) {

        ProjectMember actor = projectMemberFinder.getActiveWithWorkspaceMember(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), actorMemberId);

        projectAuthService.requireProjectManager(actor);

        Workflow workflow = workflowFinder.getWithProjectBy(
                projectIdentifier.workspaceKey(), projectIdentifier.projectKey(), workflowId);

        WorkflowTransition prOpenedTransition = null;
        if (cmd.vcsPrOpenedTransitionId() != null) {
            prOpenedTransition = workflow.getTransitions().stream()
                    .filter(t -> Objects.equals(t.getId(), cmd.vcsPrOpenedTransitionId()))
                    .findFirst()
                    .orElseThrow(() -> new WorkflowTransitionNotFoundException(
                            projectIdentifier.projectKey(), workflow.getId(), cmd.vcsPrOpenedTransitionId()));
        }

        WorkflowTransition prMergedTransition = null;
        if (cmd.vcsPrMergedTransitionId() != null) {
            prMergedTransition = workflow.getTransitions().stream()
                    .filter(t -> Objects.equals(t.getId(), cmd.vcsPrMergedTransitionId()))
                    .findFirst()
                    .orElseThrow(() -> new WorkflowTransitionNotFoundException(
                            projectIdentifier.projectKey(), workflow.getId(), cmd.vcsPrMergedTransitionId()));
        }

        workflow.updateVcsSettings(VcsAutomationSettings.of(workflow, prOpenedTransition, prMergedTransition));
    }
}
