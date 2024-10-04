package com.uranus.taskmanager.api.workspace.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.uranus.taskmanager.api.auth.LoginMember;
import com.uranus.taskmanager.api.auth.LoginRequired;
import com.uranus.taskmanager.api.auth.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.common.ApiResponse;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;
import com.uranus.taskmanager.api.workspace.service.WorkspaceCreateService;
import com.uranus.taskmanager.api.workspace.service.WorkspaceService;
import com.uranus.taskmanager.api.workspacemember.WorkspaceRole;
import com.uranus.taskmanager.api.workspacemember.authorization.RoleRequired;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

	/**
	 * Todo 1
	 * 워크스페이스 관련 모든 작업은 기본적으로 @LoginRequired 필요
	 * 워크스페이스 내의 권한 자격을 나타내는 @Role 구현
	 * 특정 워크스페이스의 이름, 설명 수정은 @Role({"ADMIN"})
	 * 특정 워크스페이스의 삭제는 @Role({"ADMIN"})
	 * Todo 2
	 * 워크스페이스 생성(비밀번호 설정 추가)
	 * 워크스페이스 이름, 설명 수정
	 * 워크스페이스 삭제
	 * 워크스페이스 비밀번호 설정(만약 없으면 설정, 있으면 수정)
	 * 멤버가 속한 워크스페이스 목록 조회(Member에서 할지 고민)
	 * 워크스페이스 멤버 초대(ADMIN만 가능)
	 * 워크스페이스 참여(워크스페이스 코드, 비밀번호를 통해)
	 */
	private final WorkspaceCreateService workspaceCreateService;
	private final WorkspaceService workspaceService;

	@LoginRequired
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	public ApiResponse<WorkspaceResponse> createWorkspace(
		@LoginMember LoginMemberDto loginMember,
		@RequestBody @Valid WorkspaceCreateRequest request) {
		WorkspaceResponse response = workspaceCreateService.createWorkspace(request, loginMember);
		return ApiResponse.created("Workspace Created", response);
	}

	@GetMapping("/{workspaceCode}")
	public ApiResponse<WorkspaceResponse> getWorkspace(@PathVariable String workspaceCode) {
		WorkspaceResponse response = workspaceService.get(workspaceCode);
		return ApiResponse.ok("Workspace Found", response);
	}

	/**
	 * RoleRequired 적용 예시
	 */
	@LoginRequired
	@RoleRequired(roles = {WorkspaceRole.ADMIN})
	@PostMapping("/{workspaceCode}/admin-only")
	public ApiResponse<LoginMemberDto> adminOnlyApi(@LoginMember LoginMemberDto loginMember,
		@PathVariable String workspaceCode) {
		return ApiResponse.ok("This is an admin-only API", loginMember);
	}
}
