package com.tissue.feature.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.service.ProjectJoinService;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.workspace.application.port.repository.InvitationQueryRepository;
import com.tissue.feature.workspace.application.service.finder.InvitationFinder;
import com.tissue.feature.workspace.domain.Invitation;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.shared.exception.base.BadRequestException;
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
class InvitationServiceTest {

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
    private InvitationQueryRepository invitationQueryRepository;

    @InjectMocks
    private InvitationService sut;

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

            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.isProcessed()).willReturn(false);
            given(invitation.getWorkspace()).willReturn(workspace);
            given(invitation.getWorkspaceRole()).willReturn(WorkspaceRole.MEMBER);
            given(workspaceJoinService.join(workspace, member, WorkspaceRole.MEMBER))
                    .willReturn(joinedMember);
            given(invitation.projectKeysNotEmpty()).willReturn(true);
            given(invitation.getProjectKeys()).willReturn(List.of("PROJ-1"));
            given(invitation.getWorkspaceKey()).willReturn("WS");
            given(projectFinder.getOptionalBy("WS", "PROJ-1")).willReturn(Optional.of(project));

            // when
            sut.accept(memberId, invitationId);

            // then
            then(invitation).should().accept();
            then(workspaceJoinService).should().join(workspace, member, WorkspaceRole.MEMBER);
            then(projectJoinService).should().join(project, joinedMember);
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

            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.isProcessed()).willReturn(false);
            given(invitation.getWorkspace()).willReturn(workspace);
            given(invitation.getWorkspaceRole()).willReturn(WorkspaceRole.MEMBER);
            given(workspaceJoinService.join(workspace, member, WorkspaceRole.MEMBER))
                    .willReturn(joinedMember);
            given(invitation.projectKeysNotEmpty()).willReturn(false);
            given(invitation.getWorkspaceKey()).willReturn("WS");

            // when
            sut.accept(memberId, invitationId);

            // then
            then(invitation).should().accept();
            then(workspaceJoinService).should().join(workspace, member, WorkspaceRole.MEMBER);
            then(projectJoinService).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("fail: if invitation is already processed throws BadRequestException")
        void failAlreadyProcessedInvitation() {
            // given
            Long memberId = 1L;
            Long invitationId = 10L;
            Member member = mock(Member.class);
            Invitation invitation = mock(Invitation.class);

            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.isProcessed()).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.accept(memberId, invitationId)).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("reject invitation")
    class RejectInvitation {

        @Test
        @DisplayName("success: rejects pending invitation")
        void successRejectInvitation() {
            // given
            Long memberId = 1L;
            Long invitationId = 10L;
            Member member = mock(Member.class);
            Invitation invitation = mock(Invitation.class);

            given(memberFinder.getActiveBy(memberId)).willReturn(member);
            given(invitationFinder.getBy(invitationId, member)).willReturn(invitation);
            given(invitation.isProcessed()).willReturn(false);

            // when
            sut.reject(memberId, invitationId);

            // then
            then(invitation).should().reject();
        }
    }
}
