package com.tissue.api.issue.workflow.domain.model;

import java.util.Map;

import org.springframework.lang.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tissue.api.common.entity.NoArchiveEntity;
import com.tissue.api.issue.workflow.domain.gaurd.GuardType;

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
import lombok.ToString;

// TODO: transition + guardType 기준으로 유니크 제약이 필요하지 않을까?
// TODO: transition + executionOrder에 대한 유니크 제약이 필요할까?
@Entity
@Getter
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransitionGuardConfig extends NoArchiveEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@ToString.Include
	private Long id;

	// 어떤 Transition에 속해있는지
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "transition_id", nullable = false)
	private WorkflowTransition transition;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50)
	@ToString.Include
	private GuardType guardType;

	// Guard별 파라미터 (JSON 형식)
	@Column(columnDefinition = "jsonb")
	private String guardParams;

	// Guard 실행 순서
	@Column(nullable = false)
	@ToString.Include
	private int executionOrder;

	public static TransitionGuardConfig create(
		@NonNull WorkflowTransition transition,
		@NonNull GuardType guardType,
		@Nullable String guardParams,
		int executionOrder
	) {
		TransitionGuardConfig config = new TransitionGuardConfig();
		config.transition = transition;
		config.guardType = guardType;
		config.guardParams = guardParams;
		config.executionOrder = executionOrder;
		return config;
	}

	// JSON 파라미터를 Map으로 파싱
	public Map<String, Object> parseParams() {
		if (guardParams == null || guardParams.isBlank()) {
			return Map.of();
		}

		try {
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(guardParams,
				new TypeReference<Map<String, Object>>() {
				});
		} catch (JsonProcessingException e) {
			return Map.of();
		}
	}

	// TODO: TransitionGuardConfig는 물리 삭제 정책
	// void softDelete() {
	// 	archive();
	// }
}
