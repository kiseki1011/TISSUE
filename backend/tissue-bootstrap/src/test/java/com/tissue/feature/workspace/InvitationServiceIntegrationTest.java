package com.tissue.feature.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.port.repository.ProjectCommandRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberQueryRepository;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.application.port.repository.InvitationCommandRepository;
import com.tissue.feature.workspace.application.port.repository.InvitationQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.service.InvitationService;
import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class InvitationServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private InvitationService invitationService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Autowired
    private WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Autowired
    private InvitationCommandRepository invitationCommandRepository;

    @Autowired
    private InvitationQueryRepository invitationQueryRepository;

    @Autowired
    private ProjectCommandRepository projectCommandRepository;

    @Autowired
    private ProjectMemberQueryRepository projectMemberQueryRepository;

    private Member invitee;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        Member owner = memberCommandRepository.save(Member.create("owner@tissue.com", "owner", "Owner"));
        workspace = workspaceRepository.save(Workspace.create("WORKSPACE", "Test Workspace", null));
        workspaceMemberCommandRepository.save(WorkspaceMember.create(owner, workspace, WorkspaceRole.OWNER));

        invitee = memberCommandRepository.save(Member.create("invitee@tissue.com", "invitee", "Invitee"));
        em.flush();
    }

    @Nested
    @DisplayName("accept invitation")
    class AcceptInvitation {

        @Test
        @DisplayName("accepting invitation creates workspace member and deletes the invitation")
        void acceptCreatesWorkspaceMember() {
            // given
            Invitation invitation =
                    invitationCommandRepository.save(Invitation.create(workspace, invitee, WorkspaceRole.MEMBER));
            em.flush();
            Long invitationId = invitation.getId();

            // when
            invitationService.accept(invitee.getId(), invitationId);
            em.flush();
            em.clear();

            // then
            assertThat(workspaceMemberQueryRepository.findByWorkspaceKeyAndMemberId("WORKSPACE", invitee.getId()))
                    .isPresent()
                    .get()
                    .satisfies(wm -> assertThat(wm.getRole()).isEqualTo(WorkspaceRole.MEMBER));
            assertThat(invitationQueryRepository.findById(invitationId)).isEmpty();
        }

        @Test
        @DisplayName("accepting invitation with project keys results in joining those projects (and workspace)")
        void acceptWithProjectKeysJoinsProjects() {
            // given
            projectCommandRepository.save(Project.create(workspace, "PROJ", "Test Project", null));

            Invitation invitation = Invitation.create(workspace, invitee, WorkspaceRole.MEMBER);
            invitation.addProjectKey("PROJ");
            invitationCommandRepository.save(invitation);
            em.flush();

            // when
            invitationService.accept(invitee.getId(), invitation.getId());
            em.flush();
            em.clear();

            // then
            assertThat(workspaceMemberQueryRepository.findByWorkspaceKeyAndMemberId("WORKSPACE", invitee.getId()))
                    .isPresent();

            assertThat(projectMemberQueryRepository.findWithWorkspaceMemberByKeysAndMemberId(
                            "WORKSPACE", "PROJ", invitee.getId()))
                    .isPresent();
        }
    }

    @Nested
    @DisplayName("reject invitation")
    class RejectInvitation {

        @Test
        @DisplayName("rejecting invitation deletes it and does not create a workspace member")
        void rejectDeletesInvitation() {
            // given
            Invitation invitation =
                    invitationCommandRepository.save(Invitation.create(workspace, invitee, WorkspaceRole.MEMBER));
            em.flush();
            Long invitationId = invitation.getId();

            // when
            invitationService.reject(invitee.getId(), invitationId);
            em.flush();
            em.clear();

            // then
            assertThat(invitationQueryRepository.findById(invitationId)).isEmpty();
            assertThat(workspaceMemberQueryRepository.findByWorkspaceKeyAndMemberId("WORKSPACE", invitee.getId()))
                    .isEmpty();
        }
    }
}
