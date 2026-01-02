package com.tissue.workflow.domain;

import static com.tissue.workflow.domain.enums.StateCategory.INITIAL;

import com.tissue.common.entity.BaseEntity;
import com.tissue.common.enums.ColorType;
import com.tissue.common.vo.Name;
import com.tissue.project.domain.Project;
import com.tissue.workflow.domain.enums.StateCategory;
import com.tissue.workflow.domain.exception.WorkflowExceptions;
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
import lombok.Getter;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@SQLRestriction("softDeleted = false")
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
     * This field is technically null during the initial construction/persistence phase.
     * However, a valid, persisted Workflow domain object must have an initial state.
     * I marked the field @Nullable for NullAway/JPA, but the getter guarantees non-nullity.
     */
    @Nullable
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initial_state_id") // mark nullable = false for DB contstraint?
    private WorkflowState initialState;

    @Column(name = "system_provided", nullable = false)
    private boolean systemProvided;

    @SuppressWarnings("NullAway.Init")
    protected Workflow() {}

    public static Workflow create(Project project, Name name, @Nullable String description, ColorType color) {
        Workflow wf = new Workflow();
        wf.project = project;
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

        ensureUniqueStateName(name);

        WorkflowState state = WorkflowState.of(name, description, color, stateCategory);
        state.attachToWorkflow(this);
        states.add(state);

        if (stateCategory.isInitial()) {
            this.initialState = state;
        }

        return state;
    }

    public WorkflowTransition addTransition(
            Name name, @Nullable String description, WorkflowState source, WorkflowState target) {

        ensureUniqueTransitionNameForSource(name, source);
        ensureNoDuplicateEdge(source, target);

        WorkflowTransition transition = WorkflowTransition.of(name, description, source, target);
        transition.attachToWorkflow(this);
        transitions.add(transition);

        return transition;
    }

    public String getDisplayName() {
        return name.getDisplay();
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
        if (!states.contains(state)) {
            throw WorkflowExceptions.initialStateBelongMismatch();
        }
        if (!state.isCategorizedAs(INITIAL)) {
            throw WorkflowExceptions.initialStateCategoryMismatch();
        }
        this.initialState = state;
    }

    public void setAsSystemProvided() {
        this.systemProvided = true;
    }

    public void rename(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }

    public void updateColor(ColorType color) {
        this.color = color;
    }

    public void deleteState(WorkflowState state) {
        if (state.getCategory().isInitial()) {
            throw WorkflowExceptions.cannotDeleteInitialState(
                    this.getId(), this.getDisplayName(), state.getDisplayName());
        }
        state.softDelete();
        states.remove(state);
    }

    public void deleteTransition(WorkflowTransition transition) {
        transitions.remove(transition);
    }

    public void renameState(WorkflowState state, Name newName) {
        if (state.getName().equals(newName)) {
            return;
        }
        ensureUniqueStateName(newName);
        state.updateName(newName);
    }

    public void renameTransition(WorkflowTransition transition, Name newName) {
        if (transition.getName().equals(newName)) {
            return;
        }
        ensureUniqueTransitionNameForSource(newName, transition.getSourceState());
        transition.updateName(newName);
    }

    public void changeStateCategory(WorkflowState state, StateCategory newCategory) {
        if (state.isCategorizedAs(newCategory)) {
            return;
        }
        if (newCategory.isInitial()) {
            this.initialState = state;
        }

        state.categorizeAs(newCategory);
    }

    public void rewireTransitionSource(WorkflowTransition transition, WorkflowState newSource) {
        transition.rewireSource(newSource);
    }

    public void rewireTransitionTarget(WorkflowTransition transition, WorkflowState newTarget) {
        transition.rewireTarget(newTarget);
    }

    public void addTransitionGuard(
            WorkflowTransition transition, GuardType guardType, @Nullable Map<String, Object> params, int order) {
        transition.addGuard(guardType, params, order);
    }

    public void clearGuardsForTransition(WorkflowTransition transition) {
        transition.clearGuards();
    }

    private void ensureNoDuplicateEdge(WorkflowState source, WorkflowState target) {
        boolean dup = transitions.stream()
                .filter(t -> !t.isArchived())
                .anyMatch(x ->
                        x.getSourceState().equals(source) && x.getTargetState().equals(target));
        if (dup) {
            throw WorkflowExceptions.duplicateTransitionEdge(source.getDisplayName(), target.getDisplayName());
        }
    }

    private void ensureUniqueStateName(Name newName) {
        boolean dup = states.stream().filter(t -> !t.isArchived()).anyMatch(s -> s.getName()
                .equals(newName));
        if (dup) {
            throw WorkflowExceptions.duplicateStateName(newName.getDisplay(), name.getDisplay(), id);
        }
    }

    private void ensureUniqueTransitionNameForSource(Name newName, WorkflowState source) {
        boolean dup = transitions.stream()
                .filter(t -> !t.isArchived())
                .filter(t -> t.getSourceState().equals(source))
                .anyMatch(t -> t.getName().equals(newName));
        if (dup) {
            throw WorkflowExceptions.duplicateTransitionName(
                    newName.getDisplay(), source.getDisplayName(), name.getDisplay(), id);
        }
    }
}
