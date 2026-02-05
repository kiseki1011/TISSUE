package com.tissue.workspace.application.service.finder;

import com.tissue.workspace.application.port.out.WorkspaceRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;
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
}
