package com.tissue.api.issue.adapter.in.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.adapter.in.web.dto.request.PerformTransitionRequest;
import com.tissue.api.issue.application.dto.response.IssueDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;
import com.tissue.api.issue.application.port.in.IssueQueryUseCase;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues")
@RequiredArgsConstructor
public class IssueQueryController {

	private final IssueQueryUseCase queryUseCase;

	// TODO: TransitionResponse 말고 새로운 걸 만들어야 함. (transitionId, displayName 모두 응답 내용에 포함)
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/transitions")
	public ApiResponse<List<TransitionDetail>> getAvailableTransitions(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@RequestBody @Valid PerformTransitionRequest request,
		@CurrentMember MemberUserDetails userDetails
	) {
		List<TransitionDetail> response = queryUseCase.getAvailableTransitions(workspaceKey, issueKey);
		return ApiResponse.created("", response);
	}

	// TODO: content의 경우 크기가 클수 있어서 캐싱 정책을 적용을 고려해야 하지 않을까?
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}")
	public ApiResponse<IssueDetail> getIssueDetails(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueDetail response = queryUseCase.getIssueDetails(workspaceKey, issueKey);
		return ApiResponse.ok("Retrieved issue details.", response);
	}

	// TODO: getReviewersForIssue(): 특정 이슈에 대한 모든 리뷰어들 조회
	// TODO: getSubcribersForIssue(): 특정 이슈에 대한 모든 구독자들 조회
	// TODO: getParticipantsForIssue(): 특정 이슈에 대한 모든 참여자(assignee, reviewers, subscribers, author) 조회

	// TODO: getIssueCustomFieldValues(): 이슈의 커스텀 필드와 해당 값들을 조회

	// TODO: getRelationsForIssue(): 특정 이슈가 가지는 모든 관계 조회(outgoing, ingoing 모두?)

	// TODO: isStoryPointUpdatable() (더 좋은 이름있다면 개선)
	//  - 이슈 생성 또는 스토리 포인트 업데이트 시 스토리 포인트 설정 가능 여부

	// TODO: getIssues() 페이징 쿼리 API(프로젝트 단위)
	//  - Issue를 조건별로 검색 가능
	//  - 조건
	//    - IssuePriority, dueAt 기간, startedAt 기간, resolvedAt 기간, IssueRelation(outgoing 기준?), Sprint 번호(예시: "SPRINT-123")
	//    - storyPoint 범위, progress 범위(optional)
	//    - currentState의 category, 프로젝트내 워크플로우의 특정 state 기준(optional)
	//    - 해당 조건들에 대한 오름차순, 내림차순이 가능해야 함
	//    - 특정 enum type 커스텀 필드에 대한 특정 선택지(optional)
	//    - 특정 역할에 따른 이슈 목록 검색도 가능해야 함. 예) ?participantId={memberId}&role=assignee
	//  - 검색어 조건: title > content > summary (우선 순위), issueKey -> 빠른 속도 검색 가능해야하고 fuzzy matching과 디바운싱도 염두
}
