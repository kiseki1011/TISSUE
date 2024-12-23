package com.tissue.api.issue.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tissue.api.common.dto.ApiResponse;
import com.tissue.api.issue.presentation.dto.request.create.CreateIssueRequest;
import com.tissue.api.issue.presentation.dto.response.create.CreateIssueResponse;
import com.tissue.api.issue.service.command.IssueCommandService;
import com.tissue.api.security.authorization.interceptor.RoleRequired;
import com.tissue.api.workspacemember.domain.WorkspaceRole;
import com.tissue.api.security.authentication.interceptor.LoginRequired;

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
	 *  - 이슈 상태 변경
	 *    - DONE으로 변경하는 경우 끝난 날짜(finishedDate) 업데이트 됨
	 *    - 처음으로 IN_PROGRESS로 변경하면 현재 날짜 시간으로 startDate가 설정 됨
	 *  - 이슈 타입 변경
	 *  - 이슈 나머지 정보 변경(우선 순위, 이름, 내용)
	 *  - 이슈 라벨 변경
	 *  - 이슈 라벨 생성(등록)
	 *    - 이름(중복 불가)
	 *    - 색상
	 *  - 라벨 수정
	 *  - 이슈 라벨 삭제
	 *  - 이슈 마감일 설정
	 *  - 부모 이슈 등록하기
	 *    - STORY, TASK, BUG는 EPIC을 부모로 가질 수 있음
	 *    - SUB_TASK는 STORY, TASK, BUG를 부모로 가질 수 있음
	 *    - EPIC > STORY, TASK, BUG > SUB_TASK
	 *  - 생성된 이슈에 대해 자식 이슈 만들기
	 *    - STORY, TASK, BUG는 SUB_TASK를 자식으로 가짐
	 *    - EPIC은 STORY, TASK, BUG를 자식으로 가짐
	 *  - EPIC <-> SUB_TASK간 자식-부모 관계는 불가. 무조건 중간 위계의 이슈를 통해 조회 필요
	 *  - 부모 이슈 해제하기
	 *  - 이슈 삭제
	 *  ---
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

		CreateIssueResponse response = issueCommandService.createIssue(
			code,
			request
		);

		return ApiResponse.ok(response.getType() + " issue created.", response);
	}
}
