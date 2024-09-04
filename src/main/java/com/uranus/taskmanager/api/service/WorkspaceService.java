package com.uranus.taskmanager.api.service;

import com.uranus.taskmanager.api.domain.workspace.Workspace;
import com.uranus.taskmanager.api.repository.WorkspaceRepository;
import com.uranus.taskmanager.api.request.WorkspaceCreateRequest;
import com.uranus.taskmanager.api.response.WorkspaceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;

    @Transactional
    public WorkspaceResponse create(WorkspaceCreateRequest request) {
        Workspace workspace = workspaceRepository.save(request.toEntity());
        log.info("[WorkspaceService.create] workspace = {}", workspace);
        return WorkspaceResponse.fromEntity(workspace);
    }

}
