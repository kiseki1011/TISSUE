package com.tissue.workspace.application.service.query;

import com.tissue.workspace.application.dto.out.query.WorkspaceDetail;
import com.tissue.workspace.application.port.in.WorkspaceQueryUseCase;
import com.tissue.workspace.application.port.out.WorkspaceQueryRepository;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

    private final WorkspaceQueryRepository workspaceQueryRepository;

    @Override
    public WorkspaceDetail getDetail(String workspaceKey) {
        Workspace workspace = workspaceQueryRepository
                .findByKey(workspaceKey)
                .orElseThrow(() -> new WorkspaceNotFoundException(workspaceKey));

        return WorkspaceDetail.from(workspace);
    }
}
