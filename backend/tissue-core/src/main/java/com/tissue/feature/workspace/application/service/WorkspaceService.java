package com.tissue.feature.workspace.application.service;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.policy.MemberPolicy;
import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.dto.response.query.DeletedWorkspaceSummary;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.feature.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.port.usecase.WorkspaceUseCase;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.application.service.publisher.WorkspaceEventPublisher;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.DuplicateWorkspaceKeyException;
import com.tissue.support.util.Patchers;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class WorkspaceService implements WorkspaceUseCase {

    private final MemberFinder memberFinder;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final MemberPolicy memberPolicy;
    private final WorkspaceFinder workspaceFinder;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;
    private final WorkspaceEventPublisher workspaceEventPublisher;

    @Override
    @Transactional
    public WorkspaceCreateResponse create(CreateWorkspaceCommand cmd, Long actorMemberId) {
        Member member = memberFinder.getActiveBy(actorMemberId);

        ensureWorkspaceKeyIsUnique(cmd.workspaceKey());

        int ownedCount = workspaceMemberFinder.countOwnedWorkspaces(member);
        int joinedCount = workspaceMemberFinder.countJoinedWorkspaces(member);

        memberPolicy.ensureCanCreateWorkspace(ownedCount, joinedCount);

        Workspace workspace = Workspace.create(cmd.workspaceKey(), cmd.name(), cmd.description());

        try {
            Workspace savedWorkspace = workspaceRepository.save(workspace);

            WorkspaceMember owner = WorkspaceMember.create(member, workspace, WorkspaceRole.OWNER);
            workspaceMemberCommandRepository.save(owner);

            return WorkspaceCreateResponse.from(savedWorkspace);

        } catch (DataIntegrityViolationException e) {
            log.warn("Workspace key collision detected for key: {}", cmd.workspaceKey(), e);
            throw new DuplicateWorkspaceKeyException(cmd.workspaceKey());
        }
    }

    @Override
    public void update(String workspaceKey, UpdateWorkspaceInfoCommand cmd, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceAdmin(actor);

        Workspace workspace = actor.getWorkspace();

        Patchers.apply(cmd.name(), workspace::updateName);
        Patchers.apply(cmd.description(), workspace::updateDescription);
    }

    @Override
    public void delete(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceOwner(actor);

        Workspace workspace = actor.getWorkspace();

        workspace.softDelete();

        workspaceEventPublisher.publishWorkspaceDeleted(workspace, actor);
    }

    @Override
    public void transferOwnership(String workspaceKey, Long targetMemberId, Long actorMemberId) {
        WorkspaceMember originalOwner = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceOwner(originalOwner);

        WorkspaceMember newOwner = workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId);

        originalOwner.getWorkspace().transferOwnership(originalOwner, newOwner);

        // TODO: WorkspaceOwnershipTransferredEvent
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDetail getDetail(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);

        return WorkspaceDetail.from(actor.getWorkspace());
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> getMyWorkspaces(Long actorMemberId) {
        List<WorkspaceMember> memberships =
                workspaceMemberQueryRepository.findAllWithWorkspaceByMemberId(actorMemberId);
        return memberships.stream().map(WorkspaceSummaryResponse::from).toList();
    }

    @Override
    public void archive(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceOwner(actor);

        Workspace workspace = actor.getWorkspace();
        workspace.archive();
    }

    @Override
    public void restoreArchived(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceOwner(actor);

        Workspace workspace = actor.getWorkspace();
        workspace.restoreArchived();
    }

    @Override
    public void restoreDeleted(String workspaceKey, Long actorMemberId) {
        WorkspaceMember actor = workspaceMemberFinder.getByWorkspaceKeyAndMemberId(workspaceKey, actorMemberId);
        workspaceAuthorizationService.requireWorkspaceOwner(actor);

        Workspace workspace = workspaceFinder.getDeletedBy(workspaceKey);
        workspace.restoreSoftDeleted();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeletedWorkspaceSummary> getMyDeletedWorkspaces(Long actorMemberId) {
        List<Workspace> deletedWorkspaces = workspaceRepository.findDeletedWorkspacesByOwnerMemberId(actorMemberId);
        return deletedWorkspaces.stream().map(DeletedWorkspaceSummary::from).toList();
    }

    private void ensureWorkspaceKeyIsUnique(String workspaceKey) {
        if (workspaceRepository.existsByKey(workspaceKey)) {
            throw new DuplicateWorkspaceKeyException(workspaceKey);
        }
    }
}
