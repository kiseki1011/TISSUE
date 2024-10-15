package com.uranus.taskmanager.api.workspace.service;

import com.uranus.taskmanager.api.authentication.dto.request.LoginMemberDto;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceResponse;

public interface WorkspaceCreateService {
	public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request, LoginMemberDto loginMember);
}
