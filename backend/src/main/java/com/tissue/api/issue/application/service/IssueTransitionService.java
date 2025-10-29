package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.application.service.TransitionGuardRegistry;
import com.tissue.api.workflow.domain.gaurd.GuardContext;
import com.tissue.api.workflow.domain.gaurd.TransitionGuard;
import com.tissue.api.workflow.domain.model.TransitionGuardConfig;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.domain.model.WorkflowState;
import com.tissue.api.workflow.domain.model.WorkflowTransition;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IssueTransitionService implements IssueTransitionUseCase {

	private final IssueFinder issueFinder;
	private final WorkflowFinder workflowFinder;
	private final TransitionGuardRegistry guardRegistry;

	/**
	 * Issue의 상태를 전이시킴
	 * <p>
	 * 실행 흐름:
	 * 1. Issue 조회
	 * 2. Transition 검증 (현재 상태가 source인지)
	 * 3. 모든 Guard 순차 실행 (하나라도 실패하면 중단)
	 * 4. 상태 전이 실행
	 * 5. 도메인 이벤트 발행
	 */
	@Override
	public IssueCommandResult performTransition(PerformTransitionCommand cmd) {
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());
		WorkflowTransition transition = findAndValidateTransition(issue, cmd.transitionId());

		// 모든 Guard 실행
		executeGuards(cmd.workspaceKey(), issue, transition, cmd.actorMemberId());

		WorkflowState previousStatus = issue.getCurrentState();

		// 상태 전이
		issue.transitionTo(transition.getTargetState());

		log.info("Issue transitioned: workspace={}, issueKey={}, transition={}, {} -> {}",
			cmd.workspaceKey(),
			cmd.issueKey(),
			transition.getLabel().getDisplay(),
			previousStatus.getLabel().getDisplay(),
			transition.getTargetState().getLabel().getDisplay()
		);

		// 도메인 이벤트 발행 (알림, 히스토리 기록...)
		// eventPublisher.publishEvent(new IssueTransitionedEvent(
		// 	issue.getId(),
		// 	issue.getKey(),
		// 	cmd.workspaceKey(),
		// 	previousStatus.getId(),
		// 	transition.getTargetStatus().getId(),
		// 	cmd.transitionId(),
		// 	cmd.actorMemberId()
		// ));

		return IssueCommandResult.from(issue);
	}

	/**
	 * Transition 찾기 및 기본 검증
	 * <p>
	 * 검증 항목:
	 * 1. Transition이 해당 Workflow에 존재하는지
	 * 2. 현재 Issue 상태가 Transition의 source status인지
	 */
	private WorkflowTransition findAndValidateTransition(
		Issue issue,
		Long transitionId
	) {
		Workflow workflow = issue.getIssueType().getWorkflow();

		// transition 조회
		WorkflowTransition transition = workflowFinder.findWorkflowTransition(workflow, transitionId);

		// 현재 상태가 이 Transition의 source status인지 확인
		// 예: 현재 "PLANNED"인데 "IN_PROGRESS -> DONE" transition 시도하면 실패
		// TODO: IssueTransitionValidator로 로직 분리
		boolean transitionSourceStateNotMatch = !issue.getCurrentState().equals(transition.getSourceState());
		if (transitionSourceStateNotMatch) {
			throw new InvalidOperationException(
				"Invalid transition. Current state is '%s' but transition requires '%s'".formatted(
					issue.getCurrentState().getDisplayLabel(),
					transition.getSourceState().getDisplayLabel()
				)
			);
		}

		return transition;
	}

	/**
	 * 모든 Guard를 순서대로 실행
	 * <p>
	 * 동작 방식:
	 * 1. Transition의 guardConfigs를 executionOrder 순으로 가져옴 (@OrderBy 적용됨)
	 * 2. 각 config에서 guardType 추출
	 * 3. Registry에서 해당 Guard 구현체 조회
	 * 4. GuardContext 생성 (issue, transition, workspace, actor, params)
	 * 5. guard.evaluate(context) 실행
	 * 6. false 반환 시 TransitionGuardException 발생 및 중단
	 * <p>
	 * Guard가 없으면 바로 통과
	 */
	private void executeGuards(
		String workspaceKey,
		Issue issue,
		WorkflowTransition transition,
		Long actorMemberId
	) {
		// Transition에 설정된 Guard Config들 (이미 executionOrder로 정렬됨)
		List<TransitionGuardConfig> configs = transition.getGuardConfigs();

		// Guard가 없으면 바로 통과
		if (configs.isEmpty()) {
			log.debug("No guards configured for transition: {}", transition.getDisplayLabel());
			return;
		}

		log.debug("Executing {} guard(s) for transition: {}",
			configs.size(), transition.getDisplayLabel());

		// 각 Guard Config에 대해 순서대로 실행
		for (TransitionGuardConfig config : configs) {
			// guardType으로 실제 Guard 구현체 조회
			// 예: GuardType.ASSIGNEE_REQUIRED -> AssigneeRequiredGuard 인스턴스
			TransitionGuard guard = guardRegistry.getGuard(config.getGuardType());

			// Guard 실행에 필요한 컨텍스트 생성
			GuardContext context = GuardContext.builder()
				.issue(issue)                      // 전이 대상 이슈
				.transition(transition)            // 실행 중인 전이
				.workspaceKey(workspaceKey)        // 워크스페이스 키
				.actorMemberId(actorMemberId)      // 행위자 멤버 ID
				.params(config.parseParams())      // JSON 파라미터를 Map으로 파싱
				.build();

			// Guard 조건 평가
			boolean failEvaluation = !guard.evaluate(context);

			if (failEvaluation) {
				// 실패 시 메시지 생성 및 예외 발생 (이후 Guard는 실행 안함)
				String message = guard.getFailureMessage(context);

				log.warn("Guard evaluation failed: guardType={}, issueKey={}, message={}",
					guard.getType(),
					issue.getKey(),
					message
				);

				throw new RuntimeException(guard.getType() + message);
			}

			log.debug("Guard evaluation passed: {}", guard.getType());
		}

		// 모든 Guard 통과
		log.debug("All guard evaluation passed for transition: {}", transition.getDisplayLabel());
	}
}
