package com.tissue.feature.workflow.domain;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.entity.HardDeleteEntity;
import com.tissue.shared.vo.Name;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(name = "workflow_transition")
public class WorkflowTransition extends HardDeleteEntity {

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Embedded
    private Name name;

    @Column(name = "description", nullable = false)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_state_id", nullable = false)
    private WorkflowState sourceState;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_state_id", nullable = false)
    private WorkflowState targetState;

    @OneToMany(mappedBy = "transition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("executionOrder ASC")
    private List<TransitionGuardConfig> guardConfigs = new ArrayList<>();

    @SuppressWarnings("NullAway.Init")
    protected WorkflowTransition() {}

    public static WorkflowTransition of(
            Name name, @Nullable String description, WorkflowState sourceState, WorkflowState targetState) {
        WorkflowTransition wt = new WorkflowTransition();
        wt.name = name;
        wt.description = Objects.requireNonNullElse(description, "");
        wt.sourceState = sourceState;
        wt.targetState = targetState;
        return wt;
    }

    void updateName(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }

    void attachToWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    void rewireSource(WorkflowState sourceState) {
        this.sourceState = sourceState;
    }

    void rewireTarget(WorkflowState targetState) {
        this.targetState = targetState;
    }

    void addGuard(GuardType guardType, @Nullable Map<String, Object> params, int order) {
        TransitionGuardConfig config = TransitionGuardConfig.create(this, guardType, params, order);
        guardConfigs.add(config);
    }

    void clearGuards() {
        guardConfigs.clear();
    }

    public String getDisplayName() {
        return name.getDisplayName();
    }
}
