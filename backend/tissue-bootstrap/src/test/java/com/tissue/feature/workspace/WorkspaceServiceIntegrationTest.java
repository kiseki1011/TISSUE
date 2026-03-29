package com.tissue.feature.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.service.WorkspaceService;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.DuplicateWorkspaceKeyException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class WorkspaceServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    private Member savedMember;

    @BeforeEach
    void setup() {
        Member member = Member.create("test@tissue.com", "testuser", "HongGildong");
        savedMember = memberCommandRepository.save(member);
        em.flush();
    }

    @Nested
    @DisplayName("create workspace")
    class CreateWorkspace {

        @Test
        @DisplayName("creating workspace also creates the 'OWNER' workspace member")
        void successCreateWorkspace() {
            // given
            String workspaceKey = "WORKSPACE";
            CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(workspaceKey, "Test Workspace", null);

            // when
            WorkspaceCreateResponse response = workspaceService.create(cmd, savedMember.getId());

            // then
            assertThat(response.workspaceKey()).isEqualTo(workspaceKey);

            WorkspaceMember owner = workspaceMemberQueryRepository
                    .findByWorkspaceKeyAndMemberId(workspaceKey, savedMember.getId())
                    .get();

            assertThat(owner.isOwner()).isTrue();
        }

        @Test
        @DisplayName("fails if workspace key exists")
        void failIfWorkspaceKeyDuplicate() {
            // given
            Workspace existingWorkspace = Workspace.create("WORKSPACE", "Test Workspace", null);
            workspaceRepository.save(existingWorkspace);
            em.flush();

            CreateWorkspaceCommand cmd = new CreateWorkspaceCommand("WORKSPACE", "Test Workspace", null);

            // when & then
            assertThatThrownBy(() -> workspaceService.create(cmd, savedMember.getId()))
                    .isInstanceOf(DuplicateWorkspaceKeyException.class);
        }
    }

    @Nested
    @DisplayName("transfer ownership")
    class TransferOwnership {

        @Test
        @DisplayName("when ownership is transferred the original 'OWNER' becomes 'ADMIN'")
        void successTransferOwnership() {
            // given
            String workspaceKey = "WORKSPACE";
            CreateWorkspaceCommand cmd = new CreateWorkspaceCommand(workspaceKey, "Test Workspace", null);
            workspaceService.create(cmd, savedMember.getId());

            Workspace workspace = workspaceRepository.findByKey(workspaceKey).get();
            WorkspaceMember owner = workspaceMemberQueryRepository
                    .findByWorkspaceKeyAndMemberId(workspaceKey, savedMember.getId())
                    .get();

            Member newMember =
                    memberCommandRepository.save(Member.create("test2@tissue.com", "testuser2", "KimChulSoo"));
            em.flush();

            WorkspaceMember newOwner = workspaceMemberCommandRepository.save(
                    WorkspaceMember.create(newMember, workspace, WorkspaceRole.MEMBER));
            em.flush();

            // when
            workspaceService.transferOwnership(workspaceKey, newMember.getId(), savedMember.getId());

            // then
            assertThat(owner.getRole()).isEqualTo(WorkspaceRole.ADMIN);
            assertThat(newOwner.isOwner()).isTrue();
        }
    }
}
