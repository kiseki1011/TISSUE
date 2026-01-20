package com.tissue.workspace.application.service.finder;

import com.tissue.workspace.application.port.out.WorkspaceQueryRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.exception.WorkspaceArchivedException;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WorkspaceFinder {

    private final WorkspaceQueryRepository workspaceQueryRepository;

    // TODO: add javadoc for the following information
    //  - its only for command API's
    //  - will throw an exception if workspace was archived
    public Workspace getModifiableBy(String workspaceKey) {
        Workspace workspace = getBy(workspaceKey);

        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspace);
        }

        return workspace;
    }

    public Workspace getModifiableBy(Long workspaceId) {
        Workspace workspace = workspaceQueryRepository
                .findById(workspaceId)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));

        if (workspace.isArchived()) {
            throw new WorkspaceArchivedException(workspace);
        }

        return workspace;
    }

    // TODO: add javadoc for the following information
    //  - its only for query API's
    public Workspace getBy(String workspaceKey) {
        return workspaceQueryRepository
                .findByKey(workspaceKey)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));
    }
}
