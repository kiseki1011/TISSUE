package com.tissue.api.workflow.domain.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tissue.api.workflow.domain.guard.GuardType;
import com.tissue.api.workflow.domain.guard.TransitionGuard;

@Component
public class TransitionGuardRegistry {

	private final Map<GuardType, TransitionGuard> guards;

	public TransitionGuardRegistry(List<TransitionGuard> guardList) {
		this.guards = guardList.stream()
			.collect(Collectors.toMap(TransitionGuard::getType, Function.identity()));
	}

	public TransitionGuard getGuard(GuardType type) {
		TransitionGuard guard = guards.get(type);
		if (guard == null) {
			throw new IllegalStateException("Unknown guard type: " + type);
		}
		return guard;
	}

	public void ensureGuardExists(GuardType type) {
		if (!guards.containsKey(type)) {
			throw new IllegalArgumentException("Unsupported guard type: " + type);
		}
	}

	public List<GuardType> getAvailableGuardTypes() {
		return new ArrayList<>(guards.keySet());
	}
}
