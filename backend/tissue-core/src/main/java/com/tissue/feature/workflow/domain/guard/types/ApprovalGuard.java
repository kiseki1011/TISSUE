package com.tissue.feature.workflow.domain.guard.types;

import com.tissue.feature.issue.domain.IssueReviewer;
import com.tissue.feature.issue.domain.enums.ReviewStatus;
import com.tissue.feature.issue.domain.exception.ReviewIncompleteException;
import com.tissue.feature.issue.domain.policy.IssuePolicy;
import com.tissue.feature.workflow.domain.exception.InvalidGuardParameterException;
import com.tissue.feature.workflow.domain.exception.TransitionGuardFailedException;
import com.tissue.feature.workflow.domain.guard.GuardContext;
import com.tissue.feature.workflow.domain.guard.GuardParamMetaData;
import com.tissue.feature.workflow.domain.guard.GuardParamType;
import com.tissue.feature.workflow.domain.guard.GuardType;
import com.tissue.feature.workflow.domain.guard.TransitionGuard;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApprovalGuard implements TransitionGuard {

    public static final String KEY_MIN_APPROVALS = "min_approvals";
    public static final String KEY_BLOCK_ON_CHANGE_REQUEST = "block_on_change_request";
    public static final String KEY_AUTO_REJECT = "auto_transition_on_reject";
    public static final String KEY_REJECT_TRANSITION = "reject_transition_name";

    private final IssuePolicy issuePolicy;

    @Override
    public GuardType getType() {
        return GuardType.REQUIRED_APPROVAL;
    }

    @Override
    public void evaluate(GuardContext context) {
        Map<String, Object> params = context.getParams();

        int minApprovals = getInt(params, KEY_MIN_APPROVALS, 1);
        boolean blockOnRequest = getBool(params, KEY_BLOCK_ON_CHANGE_REQUEST, true);

        Set<IssueReviewer> reviewers = context.getIssue().getParticipants().getReviewers();

        if (blockOnRequest) {
            boolean hasReject = reviewers.stream().anyMatch(r -> r.getStatus() == ReviewStatus.CHANGES_REQUESTED);

            if (hasReject) {
                String reason = "Transition blocked by change requests";
                throw new TransitionGuardFailedException(
                        getType(), reason, context.getIssue().getKey(), context.getWorkspaceKey());
            }
        }

        long approvedCount = reviewers.stream()
                .filter(r -> r.getStatus() == ReviewStatus.APPROVED)
                .count();

        if (approvedCount < minApprovals) {
            throw new ReviewIncompleteException(context.getIssue().getKey(), (int) approvedCount, minApprovals);
        }
    }

    @Override
    public void validateParams(Map<String, Object> params, GuardType guardType) {
        int min = getInt(params, KEY_MIN_APPROVALS, 1);
        if (min < 1) {
            String reason = "%s must be at least 1".formatted(KEY_MIN_APPROVALS);
            throw new InvalidGuardParameterException(reason, guardType);
        }

        if (min > issuePolicy.getMaxReviewers()) {
            String reason = "%s (%d) cannot exceed max reviewers (%d)"
                    .formatted(KEY_MIN_APPROVALS, min, issuePolicy.getMaxReviewers());
            throw new InvalidGuardParameterException(reason, guardType);
        }

        boolean autoReject = getBool(params, KEY_AUTO_REJECT, false);
        String rejectTransName = (String) params.get(KEY_REJECT_TRANSITION);

        if (autoReject && (rejectTransName == null || rejectTransName.isBlank())) {
            String reason = "%s is required when auto-reject is enabled".formatted(KEY_REJECT_TRANSITION);
            throw new InvalidGuardParameterException(reason, guardType);
        }
    }

    @Override
    public List<GuardParamMetaData> getParamMetaData() {
        return List.of(
                GuardParamMetaData.of(KEY_MIN_APPROVALS, GuardParamType.NUMBER, 1, true),
                GuardParamMetaData.of(KEY_BLOCK_ON_CHANGE_REQUEST, GuardParamType.BOOLEAN, true, true),
                GuardParamMetaData.of(KEY_AUTO_REJECT, GuardParamType.BOOLEAN, false, false),
                GuardParamMetaData.of(KEY_REJECT_TRANSITION, GuardParamType.TEXT, null, false));
    }

    private int getInt(Map<String, Object> params, String key, int defaultValue) {
        Object val = params.get(key);
        return (val instanceof Number n) ? n.intValue() : defaultValue;
    }

    private boolean getBool(Map<String, Object> params, String key, boolean defaultValue) {
        Object val = params.get(key);
        return (val instanceof Boolean b) ? b : defaultValue;
    }
}
