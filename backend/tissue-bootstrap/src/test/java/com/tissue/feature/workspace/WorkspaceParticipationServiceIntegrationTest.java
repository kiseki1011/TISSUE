package com.tissue.feature.workspace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.workspace.application.dto.request.InviteToWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.InviteMembersResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.service.WorkspaceParticipationService;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.exception.WorkspaceErrorCode;
import com.tissue.support.IntegrationTestSupport;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class WorkspaceParticipationServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private WorkspaceParticipationService workspaceParticipationService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    private Member owner;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        owner = memberCommandRepository.save(Member.create("owner@tissue.com", "owner", "Owner"));
        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        workspaceMemberCommandRepository.save(WorkspaceMember.create(owner, workspace, WorkspaceRole.OWNER));
        em.flush();
    }

    @Nested
    @DisplayName("invite to workspace")
    class InviteToWorkspace {

        @Test
        @DisplayName("invites new members and skips already joined members")
        void invitesAndSkipsAlreadyJoined() {
            // given
            memberCommandRepository.save(Member.create("new@tissue.com", "newuser", "HongGilDong"));
            Member alreadyJoined = memberCommandRepository.save(Member.create("joined@tissue.com", "joined", "Joined"));
            workspaceMemberCommandRepository.save(
                    WorkspaceMember.create(alreadyJoined, workspace, WorkspaceRole.MEMBER));
            em.flush();

            InviteToWorkspaceCommand cmd = new InviteToWorkspaceCommand(
                    Set.of("new@tissue.com", "joined@tissue.com"), WorkspaceRole.MEMBER, null);

            // when
            InviteMembersResponse response =
                    workspaceParticipationService.inviteToWorkspace("WORKSPACE", cmd, owner.getId());

            // then
            assertThat(response.invitedEmails()).containsExactly("new@tissue.com");
            assertThat(response.skippedEmails()).containsExactly("joined@tissue.com");
        }
    }

    @Nested
    @DisplayName("leave workspace")
    class LeaveWorkspace {

        @Test
        @DisplayName("leaving workspace also soft-deletes all corresponding project members in that workspace")
        void leaveCascadesSoftDeleteToProjectMembers() {
            // given
            Member member = memberCommandRepository.save(Member.create("test@tissue.com", "testuser", "HongGilDong"));
            WorkspaceMember workspaceMember = workspaceMemberCommandRepository.save(
                    WorkspaceMember.create(member, workspace, WorkspaceRole.MEMBER));

            Project project = Project.create(workspace, "PROJ", "Test Project", null);
            projectCommandRepository.save(project);
            projectMemberCommandRepository.save(ProjectMember.create(project, workspaceMember));
            em.flush();

            // when
            workspaceParticipationService.leave("WORKSPACE", member.getId());
            em.flush();
            em.clear();

            // then
            assertThat(workspaceMemberQueryRepository.findByWorkspaceKeyAndMemberId("WORKSPACE", member.getId()))
                    .isEmpty();

            assertThat(projectMemberQueryRepository.findWithWorkspaceMemberByKeysAndMemberId(
                            "WORKSPACE", "PROJ", member.getId()))
                    .isEmpty();
        }

        @Test
        @DisplayName("owner cannot leave workspace")
        void ownerCannotLeave() {
            // when & then
            assertThatThrownBy(() -> workspaceParticipationService.leave("WORKSPACE", owner.getId()))
                    .extracting("errorCode")
                    .isEqualTo(WorkspaceErrorCode.OWNER_CANNOT_LEAVE_WORKSPACE);
        }
    }

    @Nested
    @DisplayName("kick member")
    class KickMember {

        @Test
        @DisplayName("kicking member also soft-deletes all corresponding project members in that workspace")
        void kickCascadesSoftDeleteToProjectMembers() {
            // given
            Member target = memberCommandRepository.save(Member.create("target@tissue.com", "target", "HongGilDong"));
            WorkspaceMember targetWm = workspaceMemberCommandRepository.save(
                    WorkspaceMember.create(target, workspace, WorkspaceRole.MEMBER));

            Project project = Project.create(workspace, "PROJ", "Test Project", null);
            projectCommandRepository.save(project);
            projectMemberCommandRepository.save(ProjectMember.create(project, targetWm));
            em.flush();

            // when
            workspaceParticipationService.kick("WORKSPACE", target.getId(), owner.getId());
            em.flush();
            em.clear();

            // then
            assertThat(workspaceMemberQueryRepository.findByWorkspaceKeyAndMemberId("WORKSPACE", target.getId()))
                    .isEmpty();

            assertThat(projectMemberQueryRepository.findWithWorkspaceMemberByKeysAndMemberId(
                            "WORKSPACE", "PROJ", target.getId()))
                    .isEmpty();
        }
    }
}
