package com.tissue.api.workflow.domain.guard.types;

import static com.tissue.api.issue.domain.enums.ReviewStatus.*;
import static com.tissue.api.workflow.domain.guard.GuardParamType.*;
import static com.tissue.api.workflow.domain.guard.GuardType.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.tissue.api.issue.domain.IssueReviewer;
import com.tissue.api.workflow.domain.guard.GuardContext;
import com.tissue.api.workflow.domain.guard.GuardParamMetaData;
import com.tissue.api.workflow.domain.guard.GuardType;
import com.tissue.api.workflow.domain.guard.TransitionGuard;

@Component
public class ApprovalGuard implements TransitionGuard {

	public static final String KEY_MIN_APPROVALS = "min_approvals";
	public static final String KEY_BLOCK_ON_CHANGE_REQUEST = "block_on_change_request";
	public static final String KEY_AUTO_REJECT = "auto_transition_on_reject";
	public static final String KEY_REJECT_TRANSITION = "reject_transition_name";

	@Override
	public GuardType getType() {
		return REQUIRED_APPROVAL;
	}

	@Override
	public void evaluate(GuardContext context) {
		Map<String, Object> params = context.getParams();

		int minApprovals = getInt(params, KEY_MIN_APPROVALS, 1);
		boolean blockOnRequest = getBool(params, KEY_BLOCK_ON_CHANGE_REQUEST, true);

		Set<IssueReviewer> reviewers = context.getIssue().getParticipants().getReviewers();

		if (blockOnRequest) {
			boolean hasReject = reviewers.stream()
				.anyMatch(r -> r.getStatus() == CHANGES_REQUESTED);

			// long rejectCount = reviewers.stream()
			// 	.filter(r -> r.getStatus() == ReviewStatus.CHANGES_REQUESTED)
			// 	.count();

			if (hasReject) {
				// TODO: 예외 개선
				//  - WorkflowErrorCode.TRANSITION_GUARD_FAILED
				//  - "Transition blocked. %d reviewer(s) requested changes."
				//  - TRANSITION_BLOCKED_BY_CHANGE_REQUEST
				throw new RuntimeException("Transition blocked by change requests");
				// .addContext("guardType", getType())
				// .addContext("reason", "BLOCKED_BY_CHANGE_REQUEST")
				// .addContext("rejectCount", rejectCount);
			}
		}

		long approvedCount = reviewers.stream()
			.filter(r -> r.getStatus() == APPROVED)
			.count();

		if (approvedCount < minApprovals) {
			// TODO: 예외 개선
			//  - WorkflowErrorCode.TRANSITION_GUARD_FAILED
			//  - "Insufficient approvals. Current: %d, Required: %d."
			//  - TRANSITION_INSUFFICIENT_APPROVALS
			throw new RuntimeException("Insufficient approvals");
			// .addContext("guardType", getType())
			// .addContext("reason", "INSUFFICIENT_APPROVALS")
			// .addContext("current", approvedCount)
			// .addContext("required", minApprovals);
		}
	}

	@Override
	public void validateParams(Map<String, Object> params) {
		// TODO: minApprovals defaultValue 외부 설정값으로 설정
		int min = getInt(params, KEY_MIN_APPROVALS, 1);
		if (min < 1) {
			// TODO: 예외 개선
			//  - WorkflowErrorCode.GUARD_PARAM_VALIDATION_FAILED
			//  - 그런데 GUARD_PARAM_VALIDATION_FAILED로 퉁치는게 아니라 더 상세한 코드를 사용해야 하는거 아닌가?
			throw new RuntimeException("%s must be at least 1".formatted(KEY_MIN_APPROVALS));
		}

		boolean autoReject = getBool(params, KEY_AUTO_REJECT, false);
		String rejectTransName = (String)params.get(KEY_REJECT_TRANSITION);

		if (autoReject && (rejectTransName == null || rejectTransName.isBlank())) {
			// TODO: 예외 개선
			//  - WorkflowErrorCode.GUARD_PARAM_VALIDATION_FAILED
			//  - 그런데 GUARD_PARAM_VALIDATION_FAILED로 퉁치는게 아니라 더 상세한 코드를 사용해야 하는거 아닌가?
			throw new RuntimeException(
				"%s is required when auto-reject is enabled".formatted(KEY_REJECT_TRANSITION));
		}
	}

	@Override
	public List<GuardParamMetaData> getParamMetaData() {
		return List.of(
			GuardParamMetaData.of(KEY_MIN_APPROVALS, NUMBER, 1, true),
			GuardParamMetaData.of(KEY_BLOCK_ON_CHANGE_REQUEST, BOOLEAN, true, true),
			GuardParamMetaData.of(KEY_AUTO_REJECT, BOOLEAN, false, false),
			GuardParamMetaData.of(KEY_REJECT_TRANSITION, TEXT, null, false)
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
