package com.uranus.taskmanager.api.workspace.service;

import com.uranus.taskmanager.api.authentication.dto.LoginMember;
import com.uranus.taskmanager.api.workspace.dto.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.workspace.dto.response.WorkspaceCreateResponse;

public interface WorkspaceCreateService {
	public WorkspaceCreateResponse createWorkspace(WorkspaceCreateRequest request, LoginMember loginMember);
}
