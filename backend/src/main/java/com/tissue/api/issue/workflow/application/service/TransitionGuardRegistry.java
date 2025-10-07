package com.tissue.api.issue.workflow.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.workflow.domain.gaurd.GuardType;
import com.tissue.api.issue.workflow.domain.gaurd.TransitionGuard;

@Component
public class TransitionGuardRegistry {

	// GuardType -> TransitionGuard 매핑
	private final Map<GuardType, TransitionGuard> guards;

	public TransitionGuardRegistry(List<TransitionGuard> guardList) {
		// Stream으로 Map 생성
		// key: guard.getType() (GuardType enum)
		// value: guard 인스턴스
		this.guards = guardList.stream()
			.collect(Collectors.toMap(
				TransitionGuard::getType,
				Function.identity()
			));
	}

	// GuardType으로 Guard 인스턴스 조회
	public TransitionGuard getGuard(GuardType type) {
		TransitionGuard guard = guards.get(type);
		if (guard == null) {
			throw new IllegalStateException("Unknown guard type: " + type);
		}
		return guard;
	}

	// 사용 가능한 모든 Guard 타입 목록
	public List<GuardType> getAvailableGuardTypes() {
		return new ArrayList<>(guards.keySet());
	}
}
