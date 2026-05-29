package com.tissue.feature.workflow.application.service;

import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_WORKFLOW_NAME;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.TEMP_KEY_NOT_RESOLVED;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SystemRoleAuthorizationService;
import com.tissue.feature.member.domain.Member;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkflowCommandService implements WorkflowCommandUseCase {

    private final MemberFinder memberFinder;
    private final WorkflowFinder workflowFinder;
    private final WorkflowRepository workflowRepository;
    private final WorkflowValidator workflowValidator;
    private final WorkflowGraphValidator graphValidator;
    private final TransitionGuardRegistry guardRegistry;
    private final SystemRoleAuthorizationService systemRoleAuthorizationService;

    /**
     * Creates a new workflow with its full initial graph (states + transitions).
     *
     * <p>Workflows are managed globally (instance-wide). All nodes are identified by client
     * generated temporary keys, since nothing exists in the database yet. Transitions reference
     * source/target states by these temp keys.
     *
     * <p><b>Process:</b>
     * <ol>
     *   <li>Authorize — actor must be a system {@code ADMIN} or higher</li>
     *   <li>Validate name uniqueness</li>
     *   <li>Persist an empty Workflow entity</li>
     *   <li>Create states — map each tempKey to the created {@link WorkflowState}</li>
     *   <li>Create transitions — resolve source/target states from the tempKey map</li>
     *   <li>Validate the final graph structure</li>
     * </ol>
     *
     * <p><b>Example command shape:</b>
     * <pre>{@code
     * CreateWorkflowCommand {
     *   name: "Bug Workflow",
     *   stateDefinitions: [
     *     { tempKey: "s1", name: "Open",        category: INITIAL,   color: GRAY  },
     *     { tempKey: "s2", name: "In Progress", category: ACTIVE,    color: BLUE  },
     *     { tempKey: "s3", name: "Done",        category: COMPLETED, color: GREEN }
     *   ],
     *   transitionDefinitions: [
     *     { name: "Start",    sourceTempKey: "s1", targetTempKey: "s2" },
     *     { name: "Complete", sourceTempKey: "s2", targetTempKey: "s3" }
     *   ]
     * }
     * }</pre>
     */
    @Override
    public WorkflowCreateResponse create(CreateWorkflowCommand cmd, Long actorMemberId) {
        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        workflowValidator.ensureNameUnique(cmd.name());

        try {
            Workflow workflow = workflowRepository.save(Workflow.create(cmd.name(), cmd.description(), cmd.color()));

            Map<String, WorkflowState> stateByTempKey = new HashMap<>();

            for (var s : cmd.stateDefinitions()) {
                WorkflowState state = workflow.addState(s.name(), s.description(), s.color(), s.category());
                stateByTempKey.put(s.tempKey(), state);
            }

            for (var t : cmd.transitionDefinitions()) {
                WorkflowState source = stateByTempKey.get(t.sourceTempKey());
                WorkflowState target = stateByTempKey.get(t.targetTempKey());

                if (source == null) {
                    throw new BadRequestException(TEMP_KEY_NOT_RESOLVED);
                }
                if (target == null) {
                    throw new BadRequestException(TEMP_KEY_NOT_RESOLVED);
                }

                workflow.addTransition(t.name(), t.description(), source, target);
            }

            graphValidator.ensureValidWorkflowGraph(workflow);

            log.info(
                    "Workflow created: workflowId={}, name={}, states={}, transitions={}",
                    workflow.getId(),
                    workflow.getName(),
                    cmd.stateDefinitions().size(),
                    cmd.transitionDefinitions().size());

            return WorkflowCreateResponse.from(workflow);

        } catch (DataIntegrityViolationException e) {
            throw new ResourceConflictException(DUPLICATE_WORKFLOW_NAME);
        }
    }

    @Override
    public void update(Long workflowId, UpdateWorkflowCommand cmd, Long actorMemberId) {
        Workflow workflow = workflowFinder.getById(workflowId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        Patchers.apply(cmd.name(), newName -> {
            if (!Objects.equals(workflow.getName(), newName.toString())) {
                workflowValidator.ensureNameUnique(newName);
                workflow.rename(newName);
            }
        });
        Patchers.apply(cmd.description(), workflow::updateDescription);
        Patchers.apply(cmd.color(), workflow::updateColor);
    }

    @Override
    public void delete(Long workflowId, Long actorMemberId) {
        Workflow workflow = workflowFinder.getById(workflowId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        workflowValidator.ensureWorkflowDeletable(workflow);

        workflowRepository.delete(workflow);

        log.info("Workflow deleted: workflowId={}", workflowId);
    }

    @Override
    public void updateState(Long workflowId, Long stateId, UpdateStateCommand cmd, Long actorMemberId) {
        // verify the workflow exists before resolving its state
        workflowFinder.getById(workflowId);
        WorkflowState state = workflowFinder.getStateWithHierarchyBy(workflowId, stateId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        Patchers.apply(cmd.name(), l -> state.getWorkflow().renameState(state, l));
        Patchers.apply(cmd.description(), state::updateDescription);
        Patchers.apply(cmd.color(), state::updateColor);
    }

    @Override
    public void updateTransition(Long workflowId, Long transitionId, UpdateTransitionCommand cmd, Long actorMemberId) {
        workflowFinder.getById(workflowId);
        WorkflowTransition transition = workflowFinder.getTransitionWithHierarchyBy(workflowId, transitionId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        Patchers.apply(cmd.name(), l -> transition.getWorkflow().renameTransition(transition, l));
        Patchers.apply(cmd.description(), transition::updateDescription);
    }

    /**
     * Replaces the entire guard configuration for a single transition.
     *
     * <p>This is a full replacement operation — all existing guards on the transition
     * are cleared first, then the new guards from the command are added. This avoids complex
     * diff logic and keeps the API idempotent.
     *
     * <p>For each guard in the command:
     * <ol>
     *   <li>Verify the guard type exists in {@link TransitionGuardRegistry}</li>
     *   <li>Ensure no duplicate guard types in the same request</li>
     *   <li>Validate guard-specific parameters (example: min_approvals >= 1)</li>
     *   <li>Attach the guard to the transition</li>
     * </ol>
     */
    @Override
    public void configureTransitionGuards(
            Long workflowId, Long transitionId, ConfigureTransitionGuardsCommand cmd, Long actorMemberId) {
        Workflow workflow = workflowFinder.getById(workflowId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        WorkflowTransition transition = workflow.getTransitions().stream()
                .filter(t -> t.getId().equals(transitionId))
                .findFirst()
                .orElseThrow(() -> new WorkflowTransitionNotFoundException(workflow.getId(), transitionId));

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

        log.info(
                "Transition guards configured: workflowId={}, transitionId={}, guards={}",
                workflowId,
                transitionId,
                cmd.guards().size());
    }

    @Override
    public void updateVcsSettings(Long workflowId, UpdateWorkflowVcsSettingsCommand cmd, Long actorMemberId) {
        Workflow workflow = workflowFinder.getById(workflowId);

        Member actor = memberFinder.getActiveById(actorMemberId);
        systemRoleAuthorizationService.requireSystemAdmin(actor);

        WorkflowTransition prOpenedTransition = null;
        if (cmd.vcsPrOpenedTransitionId() != null) {
            prOpenedTransition = workflow.getTransitions().stream()
                    .filter(t -> Objects.equals(t.getId(), cmd.vcsPrOpenedTransitionId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new WorkflowTransitionNotFoundException(workflow.getId(), cmd.vcsPrOpenedTransitionId()));
        }

        WorkflowTransition prMergedTransition = null;
        if (cmd.vcsPrMergedTransitionId() != null) {
            prMergedTransition = workflow.getTransitions().stream()
                    .filter(t -> Objects.equals(t.getId(), cmd.vcsPrMergedTransitionId()))
                    .findFirst()
                    .orElseThrow(() ->
                            new WorkflowTransitionNotFoundException(workflow.getId(), cmd.vcsPrMergedTransitionId()));
        }

        workflow.updateVcsSettings(VcsAutomationSettings.of(workflow, prOpenedTransition, prMergedTransition));

        log.info(
                "VCS settings updated: workflowId={}, prOpenedTransitionId={}, prMergedTransitionId={}",
                workflowId,
                cmd.vcsPrOpenedTransitionId(),
                cmd.vcsPrMergedTransitionId());
    }
}
