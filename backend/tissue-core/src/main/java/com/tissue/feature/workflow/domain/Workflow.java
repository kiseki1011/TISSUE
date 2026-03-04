package com.tissue.feature.workflow.domain;

import static com.tissue.feature.workflow.domain.enums.StateCategory.INITIAL;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.CANNOT_DELETE_INITIAL_STATE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_STATE_NAME;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.DUPLICATE_TRANSITION_EDGE;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INITIAL_STATE_BELONG_MISMATCH;
import static com.tissue.feature.workflow.domain.exception.WorkflowErrorCode.INITIAL_STATE_CATEGORY_MISMATCH;

import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.exception.ProjectArchivedException;
import com.tissue.feature.workflow.domain.enums.StateCategory;
import com.tissue.feature.workflow.domain.exception.DuplicateTransitionNameException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.enums.ColorType;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.vo.Name;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class Workflow extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    private Project project;

    @Column(name = "project_key", nullable = false, updatable = false)
    private String projectKey;

    @Column(name = "workspace_key", nullable = false, updatable = false)
    private String workspaceKey;

    @Embedded
    private Name name;

    @Nullable
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false)
    private ColorType color;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowState> states = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTransition> transitions = new ArrayList<>();

    /**
     * This field is technically null during the initial construction/persistence phase. However, a
     * valid, persisted Workflow domain object must have an initial state. The field is marked
     * {@link Nullable} for NullAway, but the getter guarantees non-null.
     */
    @Nullable
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initial_state_id", nullable = false)
    private WorkflowState initialState;

    @Column(name = "system_provided", nullable = false)
    private boolean systemProvided;

    @Embedded
    private VcsAutomationSettings vcsSettings;

    @SuppressWarnings("NullAway.Init")
    protected Workflow() {}

    public static Workflow create(Project project, Name name, @Nullable String description, ColorType color) {
        Workflow wf = new Workflow();
        wf.project = project;
        wf.validateEditable();
        wf.projectKey = project.getKey();
        wf.workspaceKey = project.getWorkspaceKey();
        wf.name = name;
        wf.description = description;
        wf.color = color;
        wf.systemProvided = false;
        wf.vcsSettings = VcsAutomationSettings.init();

        return wf;
    }

    public WorkflowState getInitialState() {
        if (this.initialState == null) {
            throw new IllegalStateException("Workflow %d (id) has no initial state".formatted(getId()));
        }
        return this.initialState;
    }

    public WorkflowState addState(
            Name name, @Nullable String description, ColorType color, StateCategory stateCategory) {

        validateEditable();
        ensureUniqueStateName(name);

        WorkflowState state = WorkflowState.of(name, description, color, stateCategory);
        state.attachToWorkflow(this);
        states.add(state);

        if (stateCategory.isInitial()) {
            this.initialState = state;
        }

        return state;
    }

    public void addTransition(Name name, @Nullable String description, WorkflowState source, WorkflowState target) {

        validateEditable();
        ensureUniqueTransitionNameForSource(name, source);
        ensureNoDuplicateEdge(source, target);

        WorkflowTransition transition = WorkflowTransition.of(name, description, source, target);
        transition.attachToWorkflow(this);
        transitions.add(transition);
    }

    public String getName() {
        return name.toString();
    }

    public List<WorkflowState> getActiveStates() {
        return List.copyOf(states);
    }

    public List<WorkflowState> getStatesByCategory(StateCategory category) {
        return states.stream().filter(s -> s.getCategory() == category).toList();
    }

    public void setInitialState(WorkflowState state) {
        validateEditable();
        if (!states.contains(state)) {
            throw new BadRequestException(INITIAL_STATE_BELONG_MISMATCH);
        }
        if (!state.isCategorizedAs(INITIAL)) {
            throw new BadRequestException(INITIAL_STATE_CATEGORY_MISMATCH);
        }
        this.initialState = state;
    }

    public void setAsSystemProvided() {
        this.systemProvided = true;
    }

    public void rename(Name name) {
        validateEditable();
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        validateEditable();
        this.description = description;
    }

    public void updateColor(ColorType color) {
        validateEditable();
        this.color = color;
    }

    public void deleteState(WorkflowState state) {
        validateEditable();
        if (state.getCategory().isInitial()) {
            throw new BadRequestException(CANNOT_DELETE_INITIAL_STATE);
        }
        states.remove(state);
    }

    public void deleteTransition(WorkflowTransition transition) {
        validateEditable();
        transitions.remove(transition);
    }

    /**
     * Renames a child state within this workflow.
     *
     * <p>This method must be used instead of calling {@link WorkflowState#updateName(Name)} directly
     * to ensure unique constraints.</p>
     *
     * @param state   The state to rename
     * @param newName The new name to apply
     */
    public void renameState(WorkflowState state, Name newName) {
        validateEditable();
        if (Objects.equals(state.getName(), newName)) {
            return;
        }
        ensureUniqueStateName(newName);
        state.updateName(newName);
    }

    /**
     * Renames a child transition within this workflow.
     *
     * <p>This method must be used instead of calling {@link WorkflowTransition#updateName(Name)} directly
     * to ensure unique constraints.</p>
     *
     * @param transition The transition to rename
     * @param newName    The new name to apply
     */
    public void renameTransition(WorkflowTransition transition, Name newName) {
        validateEditable();
        if (Objects.equals(transition.getName(), newName)) {
            return;
        }
        ensureUniqueTransitionNameForSource(newName, transition.getSourceState());
        transition.updateName(newName);
    }

    public void changeStateCategory(WorkflowState state, StateCategory newCategory) {
        validateEditable();
        if (state.isCategorizedAs(newCategory)) {
            return;
        }
        if (newCategory.isInitial()) {
            this.initialState = state;
        }

        state.categorizeAs(newCategory);
    }

    public void rewireTransitionSource(WorkflowTransition transition, WorkflowState newSource) {
        validateEditable();
        transition.rewireSource(newSource);
    }

    public void rewireTransitionTarget(WorkflowTransition transition, WorkflowState newTarget) {
        validateEditable();
        transition.rewireTarget(newTarget);
    }

    public void addTransitionGuard(
            WorkflowTransition transition, GuardType guardType, @Nullable Map<String, Object> params, int order) {
        validateEditable();
        transition.addGuard(guardType, params, order);
    }

    public void clearGuardsForTransition(WorkflowTransition transition) {
        validateEditable();
        transition.clearGuards();
    }

    public void updateVcsSettings(VcsAutomationSettings vcsSettings) {
        validateEditable();
        this.vcsSettings = vcsSettings;
    }

    private void ensureNoDuplicateEdge(WorkflowState source, WorkflowState target) {
        boolean dup = transitions.stream()
                .anyMatch(
                        x -> Objects.equals(x.getSourceState(), source) && Objects.equals(x.getTargetState(), target));
        if (dup) {
            throw new BadRequestException(DUPLICATE_TRANSITION_EDGE);
        }
    }

    private void ensureUniqueStateName(Name newName) {
        boolean dup = states.stream().anyMatch(s -> s.getName().equals(newName));
        if (dup) {
            throw new ResourceConflictException(DUPLICATE_STATE_NAME);
        }
    }

    private void ensureUniqueTransitionNameForSource(Name newName, WorkflowState source) {
        boolean dup = transitions.stream()
                .filter(t -> Objects.equals(t.getSourceState(), source))
                .anyMatch(t -> Objects.equals(t.getName(), newName));
        if (dup) {
            throw new DuplicateTransitionNameException(
                    newName.getDisplay(), source.getDisplayName(), name.getDisplay());
        }
    }

    public void validateEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
