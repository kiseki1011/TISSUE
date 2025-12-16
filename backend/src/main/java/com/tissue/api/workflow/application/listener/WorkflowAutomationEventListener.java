package com.tissue.api.workflow.application.listener;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.enums.ReviewStatus;
import com.tissue.api.issue.domain.event.IssueReviewSubmittedEvent;
import com.tissue.api.workflow.domain.TransitionGuardConfig;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.guard.GuardType;
import com.tissue.api.workflow.domain.guard.types.ApprovalGuard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowAutomationEventListener {

	private final IssueFinder issueFinder;
	private final IssueTransitionUseCase transitionUseCase;

	@EventListener
	public void handleReviewRejected(IssueReviewSubmittedEvent event) {
		if (event.status() != ReviewStatus.CHANGES_REQUESTED) {
			return;
		}
		processAutoRejection(event);
	}

	private void processAutoRejection(IssueReviewSubmittedEvent event) {
		Issue issue = issueFinder.findBy(event.issueId());
		List<WorkflowTransition> outgoingTransitions = getOutgoingTransitions(issue);

		// 설정된 자동 반려 타겟 이름 찾기
		String targetTransitionName = findAutoRejectTargetName(outgoingTransitions)
			.orElse(null);

		// 설정이 아예 없으면 -> 정상 종료 (자동 반려는 선택 기능이므로)
		if (targetTransitionName == null) {
			return;
		}

		// 이름으로 실제 트랜지션 객체 찾기
		WorkflowTransition targetTransition = findTransitionByName(outgoingTransitions, targetTransitionName)
			// TODO: 예외 개선
			.orElseThrow(() -> new RuntimeException(
				"Auto-reject target transition '%s' not found in current state. Please check workflow configuration."
					.formatted(targetTransitionName)
			));

		// 실행 - 에러 나면 리뷰 저장도 같이 롤백
		log.info("Auto-executing reject transition '{}' for issue {}", targetTransitionName, issue.getKey());

		transitionUseCase.performTransition(new PerformTransitionCommand(
			issue.getWorkspaceKey(),
			issue.getProjectKey(),
			issue.getKey(),
			targetTransition.getId(),
			event.actorMemberId()
		));
	}

	private List<WorkflowTransition> getOutgoingTransitions(Issue issue) {
		WorkflowState currentState = issue.getCurrentState();
		return currentState.getWorkflow().getTransitions().stream()
			.filter(t -> t.getSourceState().equals(currentState))
			.toList();
	}

	private Optional<String> findAutoRejectTargetName(List<WorkflowTransition> transitions) {
		return transitions.stream()
			.flatMap(t -> t.getGuardConfigs().stream())
			.filter(config -> config.getGuardType() == GuardType.REQUIRED_APPROVAL)
			.map(TransitionGuardConfig::getGuardParams)
			.filter(this::isAutoRejectEnabled)
			.map(params -> (String)params.get(ApprovalGuard.KEY_REJECT_TRANSITION))
			.filter(Objects::nonNull)
			.findFirst();
	}

	private Optional<WorkflowTransition> findTransitionByName(List<WorkflowTransition> transitions, String name) {
		return transitions.stream()
			.filter(t -> t.getLabel().getDisplay().equals(name))
			.findFirst();
	}

	private boolean isAutoRejectEnabled(Map<String, Object> params) {
		Object val = params.get(ApprovalGuard.KEY_AUTO_REJECT);
		return (val instanceof Boolean b) ? b : false;
	}
}
