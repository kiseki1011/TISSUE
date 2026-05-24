package com.tissue.feature.workspace.application.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.service.ProjectJoinService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.application.port.repository.InvitationCommandRepository;
import com.tissue.feature.workspace.application.service.finder.InvitationFinder;
import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InvitationCommandServiceTest {

    @Mock
    private InvitationFinder invitationFinder;

    @Mock
    private MemberFinder memberFinder;

    @Mock
    private ProjectFinder projectFinder;

    @Mock
    private WorkspaceJoinService workspaceJoinService;

    @Mock
    private ProjectJoinService projectJoinService;

    @Mock
    private InvitationCommandRepository invitationCommandRepository;

    @InjectMocks
    private InvitationCommandService sut;

    @Nested
    @DisplayName("accept invitation")
    class AcceptInvitation {

        @Test
        @DisplayName("success: accepts invitation and joins workspace and projects")
        void successAcceptWithProjects() {
            // given
            Long memberId = 1L;
            Long invitationId = 10L;
            Member member = mock(Member.class);
            Invitation invitation = mock(Invitation.class);
            Workspace workspace = mock(Workspace.class);
            WorkspaceMember joinedMember = mock(WorkspaceMember.class);
            Project project = mock(Project.class);

            given(memberFinder.getActiveById(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.getWorkspace()).willReturn(workspace);
            given(invitation.getWorkspaceRole()).willReturn(WorkspaceRole.MEMBER);
            given(workspaceJoinService.join(workspace, member, WorkspaceRole.MEMBER))
                    .willReturn(joinedMember);
            given(invitation.projectKeysNotEmpty()).willReturn(true);
            given(invitation.getProjectKeys()).willReturn(List.of("PROJ-1"));
            given(invitation.getWorkspaceKey()).willReturn("WORKSPACE");
            given(projectFinder.getOptionalBy("WORKSPACE", "PROJ-1")).willReturn(Optional.of(project));

            // when
            sut.accept(memberId, invitationId);

            // then
            then(workspaceJoinService).should().join(workspace, member, WorkspaceRole.MEMBER);
            then(projectJoinService).should().join(project, joinedMember);
            then(invitationCommandRepository).should().delete(invitation);
        }

        @Test
        @DisplayName("success: accepts invitation without project keys joins workspace only")
        void successAcceptWithoutProjects() {
            // given
            Long memberId = 1L;
            Long invitationId = 10L;
            Member member = mock(Member.class);
            Invitation invitation = mock(Invitation.class);
            Workspace workspace = mock(Workspace.class);
            WorkspaceMember joinedMember = mock(WorkspaceMember.class);

            given(memberFinder.getActiveById(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.getWorkspace()).willReturn(workspace);
            given(invitation.getWorkspaceRole()).willReturn(WorkspaceRole.MEMBER);
            given(workspaceJoinService.join(workspace, member, WorkspaceRole.MEMBER))
                    .willReturn(joinedMember);
            given(invitation.projectKeysNotEmpty()).willReturn(false);
            given(invitation.getWorkspaceKey()).willReturn("WORKSPACE");

            // when
            sut.accept(memberId, invitationId);

            // then
            then(workspaceJoinService).should().join(workspace, member, WorkspaceRole.MEMBER);
            then(projectJoinService).shouldHaveNoInteractions();
            then(invitationCommandRepository).should().delete(invitation);
        }
    }

    @Nested
    @DisplayName("reject invitation")
    class RejectInvitation {

        @Test
        @DisplayName("success: rejecting invitation deletes it")
        void successRejectInvitation() {
            // given
            Long memberId = 1L;
            Long invitationId = 10L;
            Member member = mock(Member.class);
            Invitation invitation = mock(Invitation.class);

            given(memberFinder.getActiveById(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.getWorkspaceKey()).willReturn("WORKSPACE");

            // when
            sut.reject(memberId, invitationId);

            // then
            then(invitationCommandRepository).should().delete(invitation);
        }
    }
}
