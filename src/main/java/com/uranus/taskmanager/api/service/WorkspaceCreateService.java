package com.uranus.taskmanager.api.service;

import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;

public interface WorkspaceCreateService {
	public WorkspaceResponse createWorkspace(WorkspaceCreateRequest request);
}
