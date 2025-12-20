package com.tissue.workflow.domain.guard.types;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.issue.domain.IssueReviewer;
import com.tissue.issue.domain.enums.ReviewStatus;
import com.tissue.workflow.domain.exception.WorkflowExceptions;
import com.tissue.workflow.domain.guard.GuardContext;
import com.tissue.workflow.domain.guard.GuardParamMetaData;
import com.tissue.workflow.domain.guard.GuardParamType;
import com.tissue.workflow.domain.guard.GuardType;
import com.tissue.workflow.domain.guard.TransitionGuard;

@Component
public class ApprovalGuard implements TransitionGuard {

	public static final String KEY_MIN_APPROVALS = "min_approvals";
	public static final String KEY_BLOCK_ON_CHANGE_REQUEST = "block_on_change_request";
	public static final String KEY_AUTO_REJECT = "auto_transition_on_reject";
	public static final String KEY_REJECT_TRANSITION = "reject_transition_name";

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
			boolean hasReject = reviewers.stream()
				.anyMatch(r -> r.getStatus() == ReviewStatus.CHANGES_REQUESTED);

			if (hasReject) {
				throw WorkflowExceptions.transitionGuardFailed(
					getType(),
					"Transition blocked by change requests",
					context.getIssue().getKey(),
					context.getWorkspaceKey()
				);
			}
		}

		long approvedCount = reviewers.stream()
			.filter(r -> r.getStatus() == ReviewStatus.APPROVED)
			.count();

		if (approvedCount < minApprovals) {
			throw WorkflowExceptions.transitionGuardFailed(
				getType(),
				"Insufficient approvals. Current: %d, Required: %d.".formatted(approvedCount, minApprovals),
				context.getIssue().getKey(),
				context.getWorkspaceKey()
			);
		}
	}

	@Override
	public void validateParams(Map<String, Object> params) {
		// TODO: minApprovals defaultValue 외부 설정값으로 설정
		int min = getInt(params, KEY_MIN_APPROVALS, 1);
		if (min < 1) {
			throw new RuntimeException("%s must be at least 1".formatted(KEY_MIN_APPROVALS));
		}

		boolean autoReject = getBool(params, KEY_AUTO_REJECT, false);
		String rejectTransName = (String)params.get(KEY_REJECT_TRANSITION);

		if (autoReject && (rejectTransName == null || rejectTransName.isBlank())) {
			throw new RuntimeException(
				"%s is required when auto-reject is enabled".formatted(KEY_REJECT_TRANSITION));
		}
	}

	@Override
	public List<GuardParamMetaData> getParamMetaData() {
		return List.of(
			GuardParamMetaData.of(KEY_MIN_APPROVALS, GuardParamType.NUMBER, 1, true),
			GuardParamMetaData.of(KEY_BLOCK_ON_CHANGE_REQUEST, GuardParamType.BOOLEAN, true, true),
			GuardParamMetaData.of(KEY_AUTO_REJECT, GuardParamType.BOOLEAN, false, false),
			GuardParamMetaData.of(KEY_REJECT_TRANSITION, GuardParamType.TEXT, null, false)
		);
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
