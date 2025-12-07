package com.tissue.api.workflow.domain;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.lang.Nullable;

import com.tissue.api.common.entity.NoArchiveEntity;
import com.tissue.api.workflow.domain.guard.GuardType;

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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

// TODO: transition + guardType 기준으로 유니크 제약이 필요하지 않을까?
// TODO: transition + executionOrder에 대한 유니크 제약이 필요할까?
@Entity
@Getter
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
