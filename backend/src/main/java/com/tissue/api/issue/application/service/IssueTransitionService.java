package com.tissue.api.issue.application.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.tissue.api.issue.application.dto.request.PerformTransitionCommand;
import com.tissue.api.issue.application.dto.response.IssueCommandResult;
import com.tissue.api.issue.application.port.in.IssueTransitionUseCase;
import com.tissue.api.issue.application.service.finder.IssueFinder;
import com.tissue.api.issue.domain.Issue;
import com.tissue.api.issue.domain.service.validator.IssueValidator;
import com.tissue.api.workflow.application.finder.WorkflowFinder;
import com.tissue.api.workflow.application.service.TransitionGuardRegistry;
import com.tissue.api.workflow.domain.TransitionGuardConfig;
import com.tissue.api.workflow.domain.Workflow;
import com.tissue.api.workflow.domain.WorkflowState;
import com.tissue.api.workflow.domain.WorkflowTransition;
import com.tissue.api.workflow.domain.gaurd.GuardContext;
import com.tissue.api.workflow.domain.gaurd.TransitionGuard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueTransitionService implements IssueTransitionUseCase {

	private final IssueFinder issueFinder;
	private final WorkflowFinder workflowFinder;
	private final IssueValidator issueValidator;
	private final TransitionGuardRegistry guardRegistry;

	@Override
	public IssueCommandResult performTransition(PerformTransitionCommand cmd) {

		Issue issue = issueFinder.findBy(cmd.issueKey(), cmd.workspaceKey());
		Workflow workflow = issue.getIssueType().getWorkflow();
		WorkflowTransition transition = workflowFinder.findTransitionBy(workflow, cmd.transitionId());

		issueValidator.ensureValidTransition(issue, cmd.transitionId(), cmd.workspaceKey(), transition);

		executeGuards(cmd.workspaceKey(), issue, transition, cmd.actorMemberId());

		WorkflowState previousStatus = issue.getCurrentState();

		issue.transitionTo(transition.getTargetState());

		log.info("Issue transition: workspaceKey= {}, issueKey= {}, transitionId= {}, [{}] {} -> {}",
			cmd.workspaceKey(),
			cmd.issueKey(),
			transition.getId(),
			transition.getLabel().getDisplay(),
			previousStatus.getLabel().getDisplay(),
			transition.getTargetState().getLabel().getDisplay()
		);

		// TODO: IssueTransitionedEvent

		return IssueCommandResult.from(issue);
	}

	private void executeGuards(
		String workspaceKey,
		Issue issue,
		WorkflowTransition transition,
		Long actorMemberId
	) {
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
				String message = guard.getFailureMessage(context);

				log.info("Guard evaluation failed: guardType={}, issueKey={}, message={}",
					guard.getType(), issue.getKey(), message);

				// TODO: TransitionGuardEvaluationFailedException vs GuardEvaluationFailedException, 혹시 더 좋은 이름이 있나?
				//  - 필요한 컨텍스트: 실패한 가드 종류, 해당 가드 종류의 실패 메세지
				//  - GuardContext도 포함시켜야 할까? 만약 포함 시켜도, 객체를 넘기는게 아니라 풀어서 전달하는게 좋지 않을까?
				throw new RuntimeException(guard.getType() + message);
			}
			log.debug("Guard evaluation passed: {}", guard.getType());
		}
		log.debug("All guard evaluation passed for transition: {}", transition.getDisplayLabel());
	}
}
