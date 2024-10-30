package com.uranus.taskmanager.api.workspace.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.workspace.dto.WorkspaceDetail;
import com.uranus.taskmanager.api.workspace.dto.response.MyWorkspacesResponse;
import com.uranus.taskmanager.api.workspacemember.repository.WorkspaceMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceQueryService {

	private final WorkspaceMemberRepository workspaceMemberRepository;

	@Transactional(readOnly = true)
	public MyWorkspacesResponse getMyWorkspaces(LoginMemberDto loginMember) {

		String loginId = loginMember.getLoginId();

		List<WorkspaceDetail> workspaceDetails = workspaceMemberRepository.findByMemberLoginId(loginId).stream()
			.map(workspaceMember -> WorkspaceDetail.from(
				workspaceMember.getWorkspace(),
				workspaceMember.getRole()
			))
			.toList();

		return MyWorkspacesResponse.from(workspaceDetails);
	}
}
