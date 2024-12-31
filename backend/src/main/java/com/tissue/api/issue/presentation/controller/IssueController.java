package com.tissue.api.issue.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.request.update.UpdateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.presentation.dto.response.update.UpdateIssueResponse;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.security.authentication.interceptor.LoginRequired;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces/{code}/issues")
public class IssueController {

	/**
	 * Todo
	 *  아래부터는 리뷰, 댓글 기능(아마 다른 컨트롤러에서 API 정의 할 듯)
	 *  - 이슈에 리뷰어 등록하기
	 *  - 리뷰어는 리뷰 등록 가능
	 *    - 리뷰 제목
	 *    - 리뷰 내용
	 *    - 리뷰 상태: APPROVED, PENDING, CHANGES_REQUESTED
	 *    - 모든 리뷰어의 APPROVED를 받으면 해당 이슈를 DONE으로 변경 가능
	 *    - 리뷰 상태 변경 기능 필요: 해당 리뷰어 또는 ADMIN 권한 이상만 변경 가능
	 *      -> 해당 검증은 서비스에서 진행해야 할 듯
	 *  - 워크스페이스 설정으로 리뷰어 지정을 강제 할지 안할지 설정 가능
	 *  - 이슈에 댓글 등록
	 *    - 댓글 내용
	 *    - 수정 여부
	 *    - 대댓글 가능(1-Depth 까지 가능)
	 *    - 대댓글은 부모 댓글, 그리고 자식 댓글을 통해 구현(자기 참조)
	 *  - 리뷰에 대한 댓글 등록
	 *    - 이슈 댓글과 유사
	 *  - 리뷰 삭제
	 *  - 댓글 삭제
	 *  - 댓글 수정
	 */

	private final IssueCommandService issueCommandService;

	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.COLLABORATOR)
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<CreateIssueResponse> createIssue(
		@PathVariable String code,
		@RequestBody @Valid CreateIssueRequest request
	) {
		CreateIssueResponse response = issueCommandService.createIssue(code, request);

		return ApiResponse.ok(response.getType() + " issue created.", response);
	}

	/**
	 * Todo 1
	 *  - 이슈 리소스 식별을 id vs issueKey?
	 *  - 내 고민은 id로 조회하는 것을 가능하게 할지 고민 중
	 *  - Issue는 Soft Delete 적용 -> 상태 변경으로 적용
	 *    - IssueStatus에 CLOSED 또는 CANCELLED를 추가하면 될 듯
	 *  - Issue의 완전 삭제(Hard Delete)는 ADMIN 권한 이상만 할 수 있도록 구현 ㄱㄱ
	 *  <br>
	 * Todo 2
	 *  - 이슈 상태 변경에 대한 조건부 설정 구현
	 *  - Issue 상태 변경 -> review 제출이 필요 없는 경우에만 가능(설정으로 규칙 설정 가능하도록)
	 *  -> 모든 review가 APPROVED인 경우 상태를 DONE으로 변경 가능
	 */

	// @LoginRequired
	// @RoleRequired(roles = WorkspaceRole.COLLABORATOR)
	// @PatchMapping("/{issueKey}/status")
	// public ApiResponse<UpdateIssueStatusResponse> updateIssueStatus(
	// 	@PathVariable String code,
	// 	@PathVariable String issueKey,
	// 	@RequestBody @Valid UpdateIssueStatusRequest request
	// ) {
	// 	UpdateIssueStatusResponse response = issueCommandService.updateIssueStatus(code, issueKey, request);
	//
	// 	return ApiResponse.ok("Issue status updated.", response);
	// }
	@LoginRequired
	@RoleRequired(roles = WorkspaceRole.COLLABORATOR)
	@PatchMapping("/{issueKey}")
	public ApiResponse<UpdateIssueResponse> updateIssueDetails(
		@PathVariable String code,
		@PathVariable String issueKey,
		@RequestBody @Valid UpdateIssueRequest request
	) {
		UpdateIssueResponse response = issueCommandService.updateIssue(code, issueKey, request);

		return ApiResponse.ok("Issue details updated.", response);
	}

	// @LoginRequired
	// @RoleRequired(roles = WorkspaceRole.COLLABORATOR)
	// @PatchMapping("/{issueKey}/parent")
	// public ApiResponse<AssignParentIssueResponse> assignParentIssue(
	// 	@PathVariable String code,
	// 	@PathVariable String issueKey,
	// 	@RequestBody @Valid AssignParentIssueRequest request
	// ) {
	// 	AssignParentIssueResponse response = issueCommandService.assignParentIssue(code, issueKey, request);
	//
	// 	return ApiResponse.ok("Parent issue assigned.", response);
	// }
	//
	// @LoginRequired
	// @RoleRequired(roles = WorkspaceRole.COLLABORATOR)
	// @DeleteMapping("/{issueKey}/parent")
	// public ApiResponse<RemoveParentIssueResponse> removeParentIssue(
	// 	@PathVariable String code,
	// 	@PathVariable String issueKey,
	// 	@RequestBody @Valid RemoveParentIssueRequest request
	// ) {
	// 	RemoveParentIssueResponse response = issueCommandService.removeParentIssue(code, issueKey, request);
	//
	// 	return ApiResponse.ok("Parent issue relationship removed.", response);
	// }
	//
	// @LoginRequired
	// @RoleRequired(roles = WorkspaceRole.ADMIN)
	// @DeleteMapping("/{issueKey}")
	// public ApiResponse<DeleteIssueResponse> deleteIssue(
	// 	@PathVariable String code,
	// 	@PathVariable String issueKey,
	// 	@RequestBody @Valid DeleteIssueRequest request
	// ) {
	// 	DeleteIssueResponse response = issueCommandService.deleteIssue(code, issueKey, request);
	//
	// 	return ApiResponse.ok("Parent issue deleted.", response);
	// }
}
