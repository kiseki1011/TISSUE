package com.tissue.workspace.application.service.query;

import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.workspace.application.port.in.WorkspaceLinkQueryUseCase;
import com.tissue.workspace.application.port.out.WorkspaceLinkQueryRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.WorkspaceInviteLink;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class WorkspaceLinkQueryService implements WorkspaceLinkQueryUseCase {

    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final WorkspaceLinkQueryRepository linkQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    public WorkspaceInviteLinkDetail getLinkDetail(String token, WorkspaceMemberContext actorContext) {
        workspaceAuthorizationService.requireWorkspaceMember(actorContext);

        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(actorContext.workspaceKey(), token));

        WorkspaceMember linkCreator =
                workspaceMemberFinder.getIncludingSoftDeleted(link.getCreatedBy(), actorContext.workspaceKey());

        return WorkspaceInviteLinkDetail.of(link, linkCreator);
    }
}
