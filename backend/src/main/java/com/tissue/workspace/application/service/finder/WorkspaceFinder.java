package com.tissue.workspace.application.service.finder;

import com.tissue.workspace.application.port.out.WorkspaceQueryRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceFinder {

    private final WorkspaceQueryRepository workspaceQueryRepository;

    public Workspace getBy(String workspaceKey) {
        return workspaceQueryRepository
                .findByKey(workspaceKey)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
    }
}
