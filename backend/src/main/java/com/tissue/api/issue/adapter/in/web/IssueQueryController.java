package com.tissue.api.issue.adapter.in.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.issue.application.dto.response.IssueCommonDetail;
import com.tissue.api.issue.application.dto.response.IssueCustomDetail;
import com.tissue.api.issue.application.dto.response.IssueRelationsDetail;
import com.tissue.api.issue.application.dto.response.IssueReviewersDetail;
import com.tissue.api.issue.application.dto.response.IssueSubscribersDetail;
import com.tissue.api.issue.application.dto.response.TransitionDetail;
import com.tissue.api.issue.application.dto.response.info.IssueBasicInfo;
import com.tissue.api.issue.application.dto.response.info.IssueIdentificationInfo;
import com.tissue.api.issue.application.dto.response.info.ParticipantInfo;
import com.tissue.api.issue.application.port.in.IssueQueryUseCase;
import com.tissue.api.security.authentication.MemberUserDetails;
import com.tissue.api.security.authentication.resolver.CurrentMember;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.model.enums.WorkspaceRole;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceKey}/issues")
@RequiredArgsConstructor
public class IssueQueryController {

	private final IssueQueryUseCase issueQueryUseCase;

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/basic")
	public ResponseEntity<IssueBasicInfo> getBasicInfo(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueBasicInfo response = issueQueryUseCase.getBasic(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	// TODO: content의 경우 크기가 클수 있어서 캐싱 정책을 적용을 고려해야 하지 않을까?
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}")
	public ResponseEntity<IssueCommonDetail> getCommon(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCommonDetail response = issueQueryUseCase.getCommon(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/custom-fields")
	public ResponseEntity<IssueCustomDetail> getCustomFields(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueCustomDetail response = issueQueryUseCase.getCustom(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/parent")
	public ResponseEntity<IssueIdentificationInfo> getParent(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueIdentificationInfo response = issueQueryUseCase.getParent(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/children")
	public ResponseEntity<List<IssueIdentificationInfo>> getChildren(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		List<IssueIdentificationInfo> response = issueQueryUseCase.getChildren(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/relations")
	public ResponseEntity<IssueRelationsDetail> getRelations(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueRelationsDetail response = issueQueryUseCase.getRelations(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	// TODO: author vs creator 더 좋은 표현은?
	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/author")
	public ResponseEntity<ParticipantInfo> getAuthor(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		ParticipantInfo response = issueQueryUseCase.getAuthor(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/reviewers")
	public ResponseEntity<IssueReviewersDetail> getReviewers(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueReviewersDetail response = issueQueryUseCase.getReviewers(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/subscribers")
	public ResponseEntity<IssueSubscribersDetail> getSubscribers(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		IssueSubscribersDetail response = issueQueryUseCase.getSubscribers(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	@RoleRequired(role = WorkspaceRole.VIEWER)
	@GetMapping("/{issueKey}/transitions")
	public ResponseEntity<List<TransitionDetail>> getAvailableTransitions(
		@PathVariable String workspaceKey,
		@PathVariable String issueKey,
		@CurrentMember MemberUserDetails userDetails
	) {
		List<TransitionDetail> response = issueQueryUseCase.getAvailableTransitions(workspaceKey, issueKey);
		return ResponseEntity.ok(response);
	}

	// TODO: getParticipants 굳이 필요할까?
	// TODO: getIssuesByState
	// TODO: getIssuesByStateCategory

	// TODO: getIssues()
	//  - 페이징 쿼리 API(프로젝트 단위)
	//  - Issue를 조건별로 검색 가능
	//  - 조건
	//    - IssuePriority
	//    - dueAt 기간
	//    - startedAt 기간
	//    - resolvedAt 기간
	//    - IssueRelation: 예를 들어서 "ISSUE-123"가 BLOCKING하는 모든 이슈 조회
	//    - 현재 소속 Sprint 번호(예시: "SPRINT-123")
	//    - storyPoint 범위
	//    - progress 범위
	//    - 워크플로우의 특정 state 기준
	//    - currentState의 category
	//    - 해당 조건들에 대한 오름차순, 내림차순이 가능해야 함
	//    - 특정 enum type 커스텀 필드에 대한 특정 선택지로 검색
	//    - (추후 진행)tag를 구현 후에 tag에 따른 검색
	//  - 검색어 조건
	//    - title > content > summary (우선 순위)
	//    - issueKey -> 빠른 속도 검색 가능해야하고 fuzzy matching과 디바운싱도 염두

	// TODO: 특정 WorkspaceMember의 역할에 따른 이슈 목록 검색
	//  - 예) ?participantId={memberId}&role=assignee -> 특정 memberId에 대한 WorkspaceMember가 assignee인 모든 이슈들의 목록
	//  - 당연히 페이징이 가능해야겠지?

	// TODO: getComments(추후 comment 애그리거트 완료 후)
	// TODO: getHistory(추후 도메인 이벤트 도입과 ActivityLog 완료 후)

}
