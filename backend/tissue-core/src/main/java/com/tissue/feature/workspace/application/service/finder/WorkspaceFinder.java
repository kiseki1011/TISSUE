package com.tissue.feature.workspace.application.service.finder;

import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceFinder {

    private final WorkspaceRepository workspaceRepository;

    public Workspace getBy(String workspaceKey) {
        return workspaceRepository
                .findByKey(workspaceKey)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
    }

    public Workspace getDeletedBy(String workspaceKey) {
        return workspaceRepository
                .findDeletedByKey(workspaceKey)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
    }
}
