package com.tissue.feature.organization.position.application.service;

import com.tissue.feature.organization.position.application.dto.request.CreatePositionCommand;
import com.tissue.feature.organization.position.application.dto.request.UpdatePositionCommand;
import com.tissue.feature.organization.position.application.dto.response.PositionCreateResponse;
import com.tissue.feature.organization.position.application.dto.response.PositionDetail;
import com.tissue.feature.organization.position.application.dto.response.PositionDetailList;
import com.tissue.feature.organization.position.application.port.repository.PositionCommandRepository;
import com.tissue.feature.organization.position.application.port.repository.PositionQueryRepository;
import com.tissue.feature.organization.position.application.port.usecase.PositionUseCase;
import com.tissue.feature.organization.position.domain.Position;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.support.util.Patchers;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class PositionService implements PositionUseCase {

    private final PositionFinder positionFinder;
    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final PositionCommandRepository positionCommandRepository;
    private final PositionQueryRepository positionQueryRepository;
    private final PositionValidator positionValidator;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public PositionCreateResponse create(String workspaceKey, CreatePositionCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthService.requireWorkspaceAdmin(actor);

        Workspace workspace = workspaceFinder.getBy(workspaceKey);
        positionValidator.ensureUniqueName(workspace, cmd.name());

        Position position = Position.create(workspace, cmd.name(), cmd.description(), cmd.color());

        return PositionCreateResponse.from(positionCommandRepository.save(position));
    }

    @Override
    public void update(String workspaceKey, Long positionId, UpdatePositionCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthService.requireWorkspaceAdmin(actor);

        Position position = positionFinder.getWithWorkspaceBy(workspaceKey, positionId);

        Patchers.apply(cmd.name(), newName -> {
            if (position.getName().isSameAs(newName)) {
                return;
            }
            positionValidator.ensureUniqueName(position.getWorkspace(), newName);
            position.updateName(newName);
        });
        Patchers.apply(cmd.description(), position::updateDescription);
        Patchers.apply(cmd.color(), position::updateColor);
    }

    @Override
    public void delete(String workspaceKey, Long positionId, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthService.requireWorkspaceAdmin(actor);

        Position position = positionFinder.getWithWorkspaceBy(workspaceKey, positionId);
        position.ensureEditable();

        positionValidator.ensureDeletable(position);

        positionCommandRepository.delete(position);
    }

    @Override
    @Transactional(readOnly = true)
    public PositionDetail getPosition(String workspaceKey, Long positionId, Long actorMemberId) {
        workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);

        Position position = positionFinder.getBy(workspaceKey, positionId);
        return PositionDetail.from(position);
    }

    @Override
    @Transactional(readOnly = true)
    public PositionDetailList getWorkspacePositions(String workspaceKey, Long actorMemberId) {
        workspaceMemberFinder.getActiveWithWorkspace(workspaceKey, actorMemberId);

        List<Position> positions = positionQueryRepository.findAllByWorkspace_KeyOrderByCreatedAtAsc(workspaceKey);
        return PositionDetailList.from(positions);
    }
}
