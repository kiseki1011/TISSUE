package com.tissue.feature.workflow.application.dto.response;

import com.tissue.feature.workflow.domain.TransitionGuardConfig;
import com.tissue.feature.workflow.domain.guard.GuardType;
import java.util.Map;

public record GuardDetail(Long id, GuardType guardType, Map<String, Object> params, int order) {
    public static GuardDetail from(TransitionGuardConfig g) {
        return new GuardDetail(g.getId(), g.getGuardType(), g.getGuardParams(), g.getExecutionOrder());
    }
}
