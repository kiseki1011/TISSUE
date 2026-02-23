package com.tissue.feature.workflow.domain.service;

import com.tissue.feature.workflow.domain.exception.GuardNotFoundException;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class TransitionGuardRegistry {

    private final Map<GuardType, TransitionGuard> guards;

    public TransitionGuardRegistry(List<TransitionGuard> guardList) {
        Map<GuardType, TransitionGuard> enumMap = new EnumMap<>(GuardType.class);
        for (TransitionGuard guard : guardList) {
            enumMap.put(guard.getType(), guard);
        }
        this.guards = Map.copyOf(enumMap);
    }

    public TransitionGuard getGuard(GuardType type) {
        TransitionGuard guard = guards.get(type);
        if (guard == null) {
            throw new GuardNotFoundException(type);
        }
        return guard;
    }

    public void ensureGuardExists(GuardType type) {
        if (!guards.containsKey(type)) {
            throw new GuardNotFoundException(type);
        }
    }

    public List<GuardType> getAvailableGuardTypes() {
        return new ArrayList<>(guards.keySet());
    }
}
