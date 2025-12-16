package com.tissue.workflow.domain;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.Nullable;

import com.tissue.common.entity.NoArchiveEntity;
import com.tissue.workflow.domain.guard.GuardType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Entity
@Getter
@Table(
	name = "transition_guard_config",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_guard_config_type",
			columnNames = {"transition_id", "guard_type"}
		),
		@UniqueConstraint(
			name = "uk_guard_config_order",
			columnNames = {"transition_id", "execution_order"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransitionGuardConfig extends NoArchiveEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transition_id", nullable = false)
	private WorkflowTransition transition;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	private GuardType guardType;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "guard_params", columnDefinition = "jsonb")
	private Map<String, Object> guardParams = new HashMap<>();

	@Column(nullable = false)
	private int executionOrder;

	public static TransitionGuardConfig create(
		@NonNull WorkflowTransition transition,
		@NonNull GuardType guardType,
		@Nullable Map<String, Object> guardParams,
		int executionOrder
	) {
		TransitionGuardConfig config = new TransitionGuardConfig();
		config.transition = transition;
		config.guardType = guardType;
		config.guardParams = guardParams != null ? guardParams : new HashMap<>();
		config.executionOrder = executionOrder;

		return config;
	}
}
