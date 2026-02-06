package com.tissue.workflow.domain;

import static com.tissue.workflow.domain.enums.StateCategory.INITIAL;

import com.tissue.common.enums.ColorType;
import com.tissue.global.entity.BaseEntity;
import com.tissue.global.vo.Name;
import com.tissue.project.domain.Project;
import com.tissue.project.domain.exception.ProjectArchivedException;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workflow.domain.exception.CannotDeleteInitialStateException;
import com.tissue.workflow.domain.exception.DuplicateStateNameException;
import com.tissue.workflow.domain.exception.DuplicateTransitionEdgeException;
import com.tissue.workflow.domain.exception.DuplicateTransitionNameException;
import com.tissue.workflow.domain.exception.InitialStateBelongMismatchException;
import com.tissue.workflow.domain.exception.InitialStateCategoryMismatchException;
import com.tissue.workflow.domain.guard.GuardType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
public class Workflow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST)
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

        return wf;
    }

    public WorkflowState getInitialState() {
        if (this.initialState == null) {
            throw new IllegalStateException("Workflow %d (id) has no initial state".formatted(id));
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

    // TODO: im not going to use archived for Workflow(inlcuding states and transtitions)
    // TODO: is softDeleted = false is filtered out?
    public List<WorkflowState> getActiveStates() {
        return states.stream().filter(s -> !s.isArchived()).toList();
    }

    public List<WorkflowState> getStatesByCategory(StateCategory category) {
        return states.stream()
                .filter(s -> !s.isArchived() && s.getCategory() == category)
                .toList();
    }

    public void setInitialState(WorkflowState state) {
        validateEditable();
        if (!states.contains(state)) {
            throw new InitialStateBelongMismatchException();
        }
        if (!state.isCategorizedAs(INITIAL)) {
            throw new InitialStateCategoryMismatchException();
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

    // TODO: hard-delete 으로 변경
    public void deleteState(WorkflowState state) {
        validateEditable();
        if (state.getCategory().isInitial()) {
            throw new CannotDeleteInitialStateException(this.getId(), this.getName(), state.getDisplayName());
        }
        state.softDelete();
        states.remove(state);
    }

    // TODO: hard-delete 으로 변경
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
     * @param state   The state to rename.
     * @param newName The new name to apply.
     * @throws DuplicateStateNameException If a state with the same name already exists.
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
     * @param transition The transition to rename.
     * @param newName    The new name to apply.
     * @throws DuplicateTransitionNameException If a transition with the same name already exists.
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

    private void ensureNoDuplicateEdge(WorkflowState source, WorkflowState target) {
        boolean dup = transitions.stream()
                .filter(t -> !t.isArchived())
                .anyMatch(x ->
                        x.getSourceState().equals(source) && x.getTargetState().equals(target));
        if (dup) {
            throw new DuplicateTransitionEdgeException(source.getDisplayName(), target.getDisplayName());
        }
    }

    private void ensureUniqueStateName(Name newName) {
        boolean dup = states.stream().filter(t -> !t.isArchived()).anyMatch(s -> s.getName()
                .equals(newName));
        if (dup) {
            throw new DuplicateStateNameException(newName.getDisplay(), name.getDisplay(), id);
        }
    }

    private void ensureUniqueTransitionNameForSource(Name newName, WorkflowState source) {
        boolean dup = transitions.stream()
                .filter(t -> !t.isArchived())
                .filter(t -> t.getSourceState().equals(source))
                .anyMatch(t -> t.getName().equals(newName));
        if (dup) {
            throw new DuplicateTransitionNameException(
                    newName.getDisplay(), source.getDisplayName(), name.getDisplay(), id);
        }
    }

    public void validateEditable() {
        if (project.isArchived()) {
            throw new ProjectArchivedException(project.getWorkspaceKey(), project.getKey());
        }
    }
}
