package com.tissue.workflow.domain;

import com.tissue.global.entity.BaseEntity;
import com.tissue.global.vo.Name;
import com.tissue.workflow.domain.guard.GuardType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
public class WorkflowTransition extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @Embedded
    private Name name;

    @Nullable
    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private WorkflowState sourceState;

    @ManyToOne(fetch = FetchType.LAZY)
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
        wt.description = description;
        wt.sourceState = sourceState;
        wt.targetState = targetState;
        return wt;
    }

    void updateName(Name name) {
        this.name = name;
    }

    public void updateDescription(@Nullable String description) {
        this.description = description;
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
        return name.getDisplay();
    }
}
