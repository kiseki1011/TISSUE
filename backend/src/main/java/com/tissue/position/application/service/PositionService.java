package com.tissue.position.application.service;

import com.tissue.common.util.Patchers;
import com.tissue.position.application.dto.request.CreatePositionCommand;
import com.tissue.position.application.dto.request.UpdatePositionCommand;
import com.tissue.position.application.dto.response.GetPositions;
import com.tissue.position.application.dto.response.PositionCreateResponse;
import com.tissue.position.application.dto.response.PositionDetail;
import com.tissue.position.application.port.in.PositionUseCase;
import com.tissue.position.application.port.out.PositionCommandRepository;
import com.tissue.position.application.port.out.PositionQueryRepository;
import com.tissue.position.application.service.finder.PositionFinder;
import com.tissue.position.application.service.validator.PositionValidator;
import com.tissue.position.domain.Position;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.domain.Workspace;
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
    private final PositionCommandRepository positionCommandRepository;
    private final PositionQueryRepository positionQueryRepository;
    private final PositionValidator positionValidator;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public PositionCreateResponse create(CreatePositionCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());

        positionValidator.ensureUniqueName(workspace, cmd.name());

        Position position = Position.create(workspace, cmd.name(), cmd.description(), cmd.color());

        return PositionCreateResponse.from(positionCommandRepository.save(position));
    }

    @Override
    public void update(UpdatePositionCommand cmd) {
        WorkspaceMemberContext actorContext = cmd.actorContext();
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Position position = positionFinder.getBy(cmd.positionId(), workspace);

        Patchers.apply(cmd.name(), newName -> {
            if (position.getName().isSameAs(newName)) {
                return;
            }
            positionValidator.ensureUniqueName(workspace, newName);
            position.updateName(newName);
        });
        Patchers.apply(cmd.description(), position::updateDescription);
        Patchers.apply(cmd.color(), position::updateColor);
    }

    @Override
    public void delete(Long positionId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getModifiableBy(actorContext.workspaceId());
        Position position = positionFinder.getBy(positionId, workspace);

        positionValidator.ensureDeletable(position);

        positionCommandRepository.delete(position);
    }

    @Override
    @Transactional(readOnly = true)
    public PositionDetail getPositionDetail(Long positionId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceMember(actorContext);
        Position position = positionFinder.getBy(positionId, actorContext.workspaceKey());
        return PositionDetail.from(position);
    }

    @Override
    @Transactional(readOnly = true)
    public GetPositions getPositions(WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceMember(actorContext);
        List<Position> positions =
                positionQueryRepository.findAllByWorkspace_KeyOrderByCreatedAtAsc(actorContext.workspaceKey());
        return GetPositions.from(positions);
    }
}
