package com.tissue.feature.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.workspace.application.port.repository.InvitationCommandRepository;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.InvitationFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.policy.WorkspacePolicy;
import com.tissue.shared.exception.base.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("UnusedVariable")
@ExtendWith(MockitoExtension.class)
class WorkspaceParticipationServiceTest {

    @Mock
    private WorkspaceFinder workspaceFinder;

    @Mock
    private ProjectFinder projectFinder;

    @Mock
    private WorkspaceMemberFinder workspaceMemberFinder;

    @Mock
    private InvitationFinder invitationFinder;

    @Mock
    private MemberQueryRepository memberQueryRepository;

    @Mock
    private InvitationCommandRepository invitationRepository;

    @Mock
    private ProjectMemberCommandRepository projectMemberCommandRepository;

    @Mock
    private WorkspacePolicy workspacePolicy;

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    @InjectMocks
    private WorkspaceParticipationService sut;

    @Nested
    @DisplayName("leave workspace")
    class LeaveWorkspace {

        @Test
        @DisplayName("success: soft deletes member and all project members")
        void successLeaveWorkspace() {
            // given
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            WorkspaceMember actor = mock(WorkspaceMember.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);

            // when
            sut.leave(workspaceKey, actorMemberId);

            // then
            then(workspacePolicy).should().ensureCanLeaveWorkspace(actor);
            then(actor).should().softDelete();
            then(projectMemberCommandRepository)
                    .should()
                    .softDeleteAllByWorkspaceKeyAndMemberId(workspaceKey, actorMemberId);
        }

        @Test
        @DisplayName("fail: OWNER cannot leave workspace")
        void failOwnerCannotLeave() {
            // given
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            WorkspaceMember actor = mock(WorkspaceMember.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);

            willThrow(new BadRequestException(mock(com.tissue.shared.exception.ErrorCode.class)))
                    .given(workspacePolicy)
                    .ensureCanLeaveWorkspace(actor);

            // when & then
            assertThatThrownBy(() -> sut.leave(workspaceKey, actorMemberId)).isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("kick member")
    class KickWorkspaceMember {

        @Test
        @DisplayName("success: kicks target member and removes all project memberships")
        void successKickMember() {
            // given
            String workspaceKey = "WORKSPACE";
            Long targetMemberId = 2L;
            Long actorMemberId = 1L;
            WorkspaceMember actor = mock(WorkspaceMember.class);
            WorkspaceMember target = mock(WorkspaceMember.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId))
                    .willReturn(target);

            // when
            sut.kick(workspaceKey, targetMemberId, actorMemberId);

            // then
            then(workspaceAuthorizationService).should().requireWorkspaceAdmin(actor);
            then(target).should().softDelete();
            then(projectMemberCommandRepository)
                    .should()
                    .softDeleteAllByWorkspaceKeyAndMemberId(workspaceKey, targetMemberId);
        }
    }
}
