package com.tissue.workspace.application.service;

import com.tissue.member.application.service.MemberFinder;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.policy.MemberPolicy;
import com.tissue.util.Patchers;
import com.tissue.workspace.application.dto.WorkspaceMemberContext;
import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.request.UpdateWorkspaceInfoCommand;
import com.tissue.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.workspace.application.dto.response.query.WorkspaceDetail;
import com.tissue.workspace.application.dto.response.query.WorkspaceSummaryResponse;
import com.tissue.workspace.application.port.in.WorkspaceUseCase;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceMemberQueryRepository;
import com.tissue.workspace.application.port.out.WorkspaceRepository;
import com.tissue.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.DuplicateWorkspaceKeyException;
import com.tissue.workspace.domain.exception.WorkspaceNotFoundException;
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
    private final WorkspaceAuthorizationService workspaceAuthService;
    private final WorkspaceMemberQueryRepository workspaceMemberQueryRepository;
    private final WorkspaceAuthorizationService workspaceAuthorizationService;

    @Override
    @Transactional
    public WorkspaceCreateResponse create(CreateWorkspaceCommand cmd, Long memberId) {
        Member member = memberFinder.getActiveBy(memberId);

        ensureWorkspaceKeyIsUnique(cmd.workspaceKey());

        int ownedCount = workspaceMemberFinder.countOwnedWorkspacesBy(member);
        int joinedCount = workspaceMemberFinder.countJoinedWorkspacesBy(member);

        memberPolicy.ensureCanCreateWorkspace(ownedCount, joinedCount, member);

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

    private void ensureWorkspaceKeyIsUnique(String workspaceKey) {
        if (workspaceRepository.existsByKey(workspaceKey)) {
            throw new DuplicateWorkspaceKeyException(workspaceKey);
        }
    }

    @Override
    public void update(UpdateWorkspaceInfoCommand cmd, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceAdmin(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        Patchers.apply(cmd.name(), workspace::updateName);
        Patchers.apply(cmd.description(), workspace::updateDescription);
    }

    @Override
    public void delete(WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceOwner(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        workspace.softDelete();

        // TODO: 하위 project들도 cascade soft-delete 처리

        // TODO: WorkspaceDeletedEvent
        //   - Should i send notifications though?
    }

    @Override
    public void transferOwnership(Long targetMemberId, WorkspaceMemberContext actorContext) {
        workspaceAuthService.requireWorkspaceOwner(actorContext);

        Workspace workspace = workspaceFinder.getBy(actorContext.workspaceKey());

        WorkspaceMember originalOwner = workspaceMemberFinder.getBy(workspace, actorContext.memberId());
        WorkspaceMember newOwner = workspaceMemberFinder.getBy(workspace, targetMemberId);

        workspace.transferOwnership(originalOwner, newOwner);

        // TODO: WorkspaceOwnershipTransferredEvent
    }

    @Override
    @Transactional(readOnly = true)
    public WorkspaceDetail getDetail(WorkspaceMemberContext actorContext) {
        workspaceAuthorizationService.requireWorkspaceMember(actorContext);

        Workspace workspace = workspaceRepository
                .findByKey(actorContext.workspaceKey())
                .orElseThrow(() -> new WorkspaceNotFoundException(actorContext.workspaceKey()));

        return WorkspaceDetail.from(workspace);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkspaceSummaryResponse> getMyWorkspaces(Long memberId) {
        List<WorkspaceMember> memberships = workspaceMemberQueryRepository.findAllWithWorkspaceByMemberId(memberId);
        return memberships.stream().map(WorkspaceSummaryResponse::from).toList();
    }
}
