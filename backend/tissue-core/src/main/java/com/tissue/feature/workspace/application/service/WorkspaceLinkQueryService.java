package com.tissue.feature.workspace.application.service;

import com.tissue.feature.workspace.application.dto.response.query.WorkspaceInviteLinkDetail;
import com.tissue.feature.workspace.application.port.repository.WorkspaceLinkQueryRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceLinkQueryUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceInviteLink;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.exception.WorkspaceInviteLinkNotFoundException;
import java.util.List;
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
    public WorkspaceInviteLinkDetail getLinkDetail(String workspaceKey, String token, Long actorMemberId) {
        workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        WorkspaceInviteLink link = linkQueryRepository
                .findByToken(token)
                .orElseThrow(() -> new WorkspaceInviteLinkNotFoundException(workspaceKey, token));

        WorkspaceMember linkCreator = workspaceMemberFinder.getWithWorkspace(workspaceKey, link.getCreatedBy());

        return WorkspaceInviteLinkDetail.of(link, linkCreator);
    }

    @Override
    public List<WorkspaceInviteLinkDetail> getWorkspaceLinks(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        List<WorkspaceInviteLink> links = linkQueryRepository.findAllByWorkspaceKey(workspaceKey);

        return links.stream()
                .map(link -> {
                    WorkspaceMember linkCreator =
                            workspaceMemberFinder.getWithWorkspace(workspaceKey, link.getCreatedBy());
                    return WorkspaceInviteLinkDetail.of(link, linkCreator);
                })
                .toList();
    }
}
