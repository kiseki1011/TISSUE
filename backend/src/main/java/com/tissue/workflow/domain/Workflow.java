package com.tissue.workflow.domain;

import static com.tissue.workflow.domain.enums.StateCategory.*;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.lang.Nullable;

@Entity
@SQLRestriction("softDeleted = false")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ColorType color;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.PERSIST)
    private List<WorkflowState> states = new ArrayList<>();

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkflowTransition> transitions = new ArrayList<>();

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initial_state_id")
    private WorkflowState initialState;

    @Column(nullable = false)
    private boolean systemProvided;

    public static Workflow create(
            @NonNull Project project, @NonNull Name name, @Nullable String description, @NonNull ColorType color) {
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

    public WorkflowState addState(
            @NonNull Name name,
            @Nullable String description,
            @NonNull ColorType color,
            @NonNull StateCategory stateCategory) {
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
            @NonNull Name name,
            @Nullable String description,
            @NonNull WorkflowState source,
            @NonNull WorkflowState target) {
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

    public List<WorkflowState> getActiveStates() {
        return states.stream().filter(s -> !s.isArchived()).toList();
    }

    public List<WorkflowState> getStatesByCategory(StateCategory category) {
        return states.stream()
                .filter(s -> !s.isArchived() && s.getCategory() == category)
                .toList();
    }

    public void setInitialState(@NonNull WorkflowState state) {
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

    public void rename(@NonNull Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
    }

    public void updateColor(@Nullable ColorType color) {
        this.color = color;
    }

    public void deleteState(@NonNull WorkflowState state) {
        if (state.getCategory().isInitial()) {
            throw WorkflowExceptions.cannotDeleteInitialState(
                    this.getId(), this.getDisplayName(), state.getDisplayName());
        }
        state.softDelete();
        states.remove(state);
    }

    public void deleteTransition(@NonNull WorkflowTransition transition) {
        transitions.remove(transition);
    }

    public void renameState(@NonNull WorkflowState state, @NonNull Name newName) {
        if (state.getName().equals(newName)) {
            return;
        }
        ensureUniqueStateName(newName);
        state.updateName(newName);
    }

    public void renameTransition(@NonNull WorkflowTransition transition, @NonNull Name newName) {
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
        if (state.getCategory().isInitial()) {
            this.initialState = null;
        }

        state.categorizeAs(newCategory);
    }

    public void rewireTransitionSource(@NonNull WorkflowTransition transition, @NonNull WorkflowState newSource) {
        transition.rewireSource(newSource);
    }

    public void rewireTransitionTarget(@NonNull WorkflowTransition transition, @NonNull WorkflowState newTarget) {
        transition.rewireTarget(newTarget);
    }

    public void addTransitionGuard(
            @NonNull WorkflowTransition transition,
            @NonNull GuardType guardType,
            @Nullable Map<String, Object> params,
            int order) {
        transition.addGuard(guardType, params, order);
    }

    public void clearGuardsForTransition(@NonNull WorkflowTransition transition) {
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
