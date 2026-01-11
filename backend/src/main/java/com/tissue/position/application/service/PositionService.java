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
import com.tissue.security.authentication.application.port.out.CurrentMemberProvider;
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
    private final CurrentMemberProvider currentMemberProvider;
    private final WorkspaceAuthorizationService workspaceAuthService;

    @Override
    public PositionCreateResponse create(CreatePositionCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(cmd.workspaceKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());

        positionValidator.ensureUniqueName(workspace, cmd.name());

        Position position = Position.builder()
                .workspace(workspace)
                .name(cmd.name())
                .description(cmd.description())
                .color(cmd.color())
                .build();

        return PositionCreateResponse.from(positionCommandRepository.save(position));
    }

    @Override
    public void update(UpdatePositionCommand cmd) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(cmd.workspaceKey(), actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(cmd.workspaceKey());
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
    public void delete(String workspaceKey, Long positionId) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceAdmin(workspaceKey, actorMemberId);

        Workspace workspace = workspaceFinder.getModifiableBy(workspaceKey);
        Position position = positionFinder.getBy(positionId, workspace);

        positionValidator.ensureDeletable(position);

        positionCommandRepository.delete(position);
    }

    @Override
    @Transactional(readOnly = true)
    public PositionDetail getPositionDetail(String workspaceKey, Long positionId) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceMember(workspaceKey, actorMemberId);

        Position position = positionFinder.getBy(positionId, workspaceKey);
        return PositionDetail.from(position);
    }

    @Override
    @Transactional(readOnly = true)
    public GetPositions getPositions(String workspaceKey) {
        Long actorMemberId = currentMemberProvider.getCurrentMemberId();
        workspaceAuthService.requireWorkspaceMember(workspaceKey, actorMemberId);

        List<Position> positions = positionQueryRepository.findAllByWorkspace_KeyOrderByCreatedAtAsc(workspaceKey);
        return GetPositions.from(positions);
    }
}
