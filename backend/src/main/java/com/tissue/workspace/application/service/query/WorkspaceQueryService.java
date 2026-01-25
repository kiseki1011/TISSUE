package com.tissue.workspace.application.service.query;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.out.query.WorkspaceDetail;
import com.tissue.workspace.application.dto.out.query.WorkspaceSummaryResponse;
import com.tissue.workspace.application.port.in.WorkspaceQueryUseCase;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceQueryService implements WorkspaceQueryUseCase {

    private final WorkspaceQueryRepository workspaceQueryRepository;
    private final WorkspaceMemberQueryRepository workspaceMemberRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public WorkspaceDetail getDetail(WorkspaceMemberContext actorContext) {
        workspaceAuthorizationService.requireWorkspaceMember(actorContext);

        Workspace workspace = workspaceQueryRepository
                .findByKey(actorContext.workspaceKey())
                .orElseThrow(() -> new WorkspaceNotFoundException(actorContext.workspaceKey()));

        return WorkspaceDetail.from(workspace);
    }

    @Override
    public List<WorkspaceSummaryResponse> getMyWorkspaces(Long memberId) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findAllByMemberIdWithWorkspace(memberId);

        return memberships.stream().map(WorkspaceSummaryResponse::from).toList();
    }
}
