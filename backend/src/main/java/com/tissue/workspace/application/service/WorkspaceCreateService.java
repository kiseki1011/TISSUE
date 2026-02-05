package com.tissue.workspace.application.service;

import com.tissue.member.application.service.MemberFinder;
import com.tissue.member.domain.Member;
import com.tissue.member.domain.policy.MemberPolicy;
import com.tissue.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.workspace.application.port.in.WorkspaceCreateUseCase;
import com.tissue.workspace.application.port.out.WorkspaceMemberCommandRepository;
import com.tissue.workspace.application.port.out.WorkspaceRepository;
import com.tissue.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.workspace.domain.Workspace;
import com.tissue.workspace.domain.WorkspaceMember;
import com.tissue.workspace.domain.enums.WorkspaceRole;
import com.tissue.workspace.domain.exception.DuplicateWorkspaceKeyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkspaceCreateService implements WorkspaceCreateUseCase {

    private final MemberFinder memberFinder;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberCommandRepository workspaceMemberCommandRepository;
    private final WorkspaceMemberFinder workspaceMemberFinder;
    private final MemberPolicy memberPolicy;

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
}
