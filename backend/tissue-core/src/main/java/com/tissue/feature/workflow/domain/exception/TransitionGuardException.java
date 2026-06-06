package com.tissue.feature.workflow.domain.exception;

import static com.tissue.shared.exception.ErrorContextKeys.GUARD_TYPE;
import static com.tissue.shared.exception.ErrorContextKeys.ISSUE_KEY;
import static com.tissue.shared.exception.ErrorContextKeys.REASON;

import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.shared.exception.ErrorCode;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;

public abstract class TransitionGuardException extends BadRequestException {

    @Getter
    private final GuardType guardType;

    private final Map<String, Object> details = new LinkedHashMap<>();

    protected TransitionGuardException(
            ErrorCode errorCode, GuardType guardType, String violationMessage, String issueKey) {
        super(errorCode, violationMessage);
        this.guardType = guardType;
        addContext(GUARD_TYPE, guardType);
        addContext(ISSUE_KEY, issueKey);
        addContext(REASON, violationMessage);
    }

    /**
     * Reason this guard blocked the transition
     */
    public String getViolationMessage() {
        String detail = getDetailMessage();
        if (detail != null && !detail.isBlank()) {
            return detail;
        }
        String message = getMessage();
        return message != null ? message : guardType.name();
    }

    public Map<String, Object> getDetails() {
        return Collections.unmodifiableMap(details);
    }

    protected void addDetail(String key, Object value) {
        details.put(key, value);
        addContext(key, value);
    }
}
