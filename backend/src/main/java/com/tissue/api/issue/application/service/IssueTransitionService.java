package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tissue.api.common.exception.type.InvalidOperationException;
import com.tissue.api.issue.application.dto.ExecuteTransitionCommand;
import com.tissue.api.issue.application.finder.IssueFinder;
import com.tissue.api.issue.domain.model.Issue;
import com.tissue.api.issue.presentation.dto.response.IssueResponse;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.application.service.TransitionGuardRegistry;
import com.tissue.api.workflow.domain.gaurd.GuardContext;
import com.tissue.api.workflow.domain.gaurd.TransitionGuard;
import com.tissue.api.workflow.domain.model.TransitionGuardConfig;
import com.tissue.api.workflow.domain.model.Workflow;
import com.tissue.api.workflow.domain.model.WorkflowStatus;
import com.tissue.api.workflow.domain.model.WorkflowTransition;
import com.tissue.api.workflow.presentation.dto.response.TransitionResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueTransitionService {

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
	 *
	 * @param cmd Transition 실행 커맨드
	 * @return 전이 완료된 Issue 정보
	 * @throws NotFoundException Transition을 찾을 수 없음
	 * @throws InvalidOperationException 현재 상태에서 실행 불가능한 Transition
	 * @throws TransitionGuardException Guard 조건 미충족
	 */
	@Transactional
	public IssueResponse executeTransition(ExecuteTransitionCommand cmd) {
		// Issue 조회 (workspace 검증 포함)
		Issue issue = issueFinder.findIssue(cmd.issueKey(), cmd.workspaceKey());

		// Transition 조회 및 기본 검증
		WorkflowTransition transition = findAndValidateTransition(issue, cmd.transitionId());

		// 모든 Guard 실행
		executeGuards(cmd.workspaceKey(), issue, transition, cmd.actorMemberId());

		// 상태 전이
		WorkflowStatus previousStatus = issue.getCurrentStatus();
		// TODO: moveToStep의 내부 구현은 사실상 set 메서드나 다름 없음.
		//  안에 검증 로직을 캡슐화 하거나 할 필요는 없을까? 물론 findAndValidateTransition에서 가능한 전이를 검증하긴 하지만
		//  더 우아하게 처리할 방법은 없나 고민이 됨.
		issue.moveToStep(transition.getTargetStatus());

		log.info("Issue transitioned: workspace={}, issueKey={}, transition={}, {} -> {}",
			cmd.workspaceKey(),
			cmd.issueKey(),
			transition.getLabel().getDisplay(),
			previousStatus.getLabel().getDisplay(),
			transition.getTargetStatus().getLabel().getDisplay()
		);

		// 5. 도메인 이벤트 발행 (알림, 히스토리 기록...)
		// eventPublisher.publishEvent(new IssueTransitionedEvent(
		// 	issue.getId(),
		// 	issue.getKey(),
		// 	cmd.workspaceKey(),
		// 	previousStatus.getId(),
		// 	transition.getTargetStatus().getId(),
		// 	cmd.transitionId(),
		// 	cmd.actorMemberId()
		// ));

		return IssueResponse.from(issue);
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
		// Issue의 IssueType에 설정된 Workflow 가져오기
		Workflow workflow = issue.getIssueType().getWorkflow();

		// transition 조회
		WorkflowTransition transition = workflowFinder.findWorkflowTransition(workflow, transitionId);

		// 현재 상태가 이 Transition의 source status인지 확인
		// 예: 현재 "TODO"인데 "IN_PROGRESS -> DONE" transition 시도하면 실패
		// TODO: IssueTransitionValidator로 로직 분리
		if (!issue.getCurrentStatus().equals(transition.getSourceStatus())) {
			throw new InvalidOperationException(
				String.format(
					"Invalid transition. Current status is '%s' but transition requires '%s'",
					issue.getCurrentStatus().getLabel().getDisplay(),
					transition.getSourceStatus().getLabel().getDisplay()
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
			log.debug("No guards configured for transition: {}", transition.getLabel().getDisplay());
			return;
		}

		log.debug("Executing {} guard(s) for transition: {}",
			configs.size(), transition.getLabel().getDisplay());

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
			if (!guard.evaluate(context)) {
				// 실패 시 메시지 생성 및 예외 발생 (이후 Guard는 실행 안함)
				String message = guard.getFailureMessage(context);

				log.warn("Guard evaluation failed: guardType={}, issueKey={}, message={}", guard.getType(),
					issue.getKey(),
					message);

				throw new RuntimeException(guard.getType() + message);
			}

			log.debug("Guard evaluation passed: {}", guard.getType());
		}

		// 모든 Guard 통과
		log.debug("All guard evaluation passed for transition: {}", transition.getLabel().getDisplay());
	}

	// TODO: IssueQueryService로

	/**
	 * 현재 상태에서 가능한 모든 Transition 조회
	 * <p>
	 * 반환되는 Transition:
	 * - Issue의 currentStatus를 source로 하는 Transition들
	 * - Guard 조건은 체크하지 않음 (실제 실행 시 체크)
	 *
	 * @param workspaceKey 워크스페이스 키
	 * @param issueKey 이슈 키
	 * @return 가능한 Transition 목록
	 */
	@Transactional(readOnly = true)
	public List<TransitionResponse> getAvailableTransitions(
		String workspaceKey,
		String issueKey
	) {
		// Issue 조회
		Issue issue = issueFinder.findIssue(issueKey, workspaceKey);

		// Issue의 Workflow 가져오기
		Workflow workflow = issue.getIssueType().getWorkflow();

		// Workflow의 모든 Transition 중에서
		return workflow.getTransitions().stream()
			// 현재 상태(currentStatus)에서 출발하는 Transition만 필터링
			// 예: 현재 "IN_PROGRESS"면 "IN_PROGRESS -> X" 형태만 선택
			.filter(t -> t.getSourceStatus().equals(issue.getCurrentStatus()))
			.map(TransitionResponse::from)
			.toList();
	}
}
