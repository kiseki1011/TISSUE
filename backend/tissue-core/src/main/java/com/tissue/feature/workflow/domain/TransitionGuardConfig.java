package com.tissue.feature.workflow.domain;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.entity.HardDeleteEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

@Entity
@Getter
@Table(
        name = "transition_guard_config",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_guard_config_type",
                    columnNames = {"transition_id", "guard_type"}),
            @UniqueConstraint(
                    name = "uk_guard_config_order",
                    columnNames = {"transition_id", "execution_order"})
        })
public class TransitionGuardConfig extends HardDeleteEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transition_id", nullable = false)
    private WorkflowTransition transition;

    @Enumerated(EnumType.STRING)
    @Column(name = "guard_type", nullable = false)
    private GuardType guardType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "guard_params", columnDefinition = "jsonb")
    private Map<String, Object> guardParams = new HashMap<>();

    @Column(nullable = false)
    private int executionOrder;

    @SuppressWarnings("NullAway.Init")
    protected TransitionGuardConfig() {}

    public static TransitionGuardConfig create(
            WorkflowTransition transition,
            GuardType guardType,
            @Nullable Map<String, Object> guardParams,
            int executionOrder) {
        TransitionGuardConfig config = new TransitionGuardConfig();
        config.transition = transition;
        config.guardType = guardType;
        config.guardParams = guardParams != null ? guardParams : new HashMap<>();
        config.executionOrder = executionOrder;

        return config;
    }
}
