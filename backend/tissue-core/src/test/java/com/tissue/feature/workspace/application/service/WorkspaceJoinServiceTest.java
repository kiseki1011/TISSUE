package com.tissue.feature.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.policy.MemberPolicy;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.enums.WorkspaceRole;
import com.tissue.feature.workspace.domain.policy.WorkspacePolicy;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceJoinServiceTest {

    @Mock
    private WorkspaceMemberFinder workspaceMemberFinder;

    @Mock
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Mock
    private WorkspacePolicy workspacePolicy;

    @Mock
    private MemberPolicy memberPolicy;

    @InjectMocks
    private WorkspaceJoinService sut;

    @Nested
    @DisplayName("join workspace")
    class JoinWorkspace {

        @Test
        @DisplayName("success: new workspace member is saved after capacity checks")
        void successNewMemberSaved() {
            // given
            Workspace workspace = mock(Workspace.class);
            Member member = mock(Member.class);
            given(workspace.getKey()).willReturn("WORKSPACE");

            given(workspaceMemberFinder.getOptionalIncludingSoftDeleted(workspace, member))
                    .willReturn(Optional.empty());
            given(workspaceMemberFinder.countTotalMembersIncludingSoftDeleted("WORKSPACE"))
                    .willReturn(5);
            given(workspaceMemberFinder.countJoinedWorkspaces(member)).willReturn(2);

            WorkspaceMember savedMember = mock(WorkspaceMember.class);
            given(workspaceMemberCommandRepository.save(any(WorkspaceMember.class)))
                    .willReturn(savedMember);

            // when
            WorkspaceMember result = sut.join(workspace, member, WorkspaceRole.MEMBER);

            // then
            assertThat(result).isEqualTo(savedMember);
            then(workspacePolicy).should().ensureCanAddMember(5);
            then(memberPolicy).should().ensureCanJoinWorkspace(2);
            then(workspaceMemberCommandRepository).should().save(any(WorkspaceMember.class));
        }

        @Test
        @DisplayName("success: existing active worksapce member returns without capacity checks")
        void successExistingActiveWorkspaceMemberReturned() {
            // given
            Workspace workspace = mock(Workspace.class);
            Member member = mock(Member.class);
            WorkspaceMember existingMember = mock(WorkspaceMember.class);

            given(workspaceMemberFinder.getOptionalIncludingSoftDeleted(workspace, member))
                    .willReturn(Optional.of(existingMember));
            given(existingMember.isSoftDeleted()).willReturn(false);

            // when
            WorkspaceMember result = sut.join(workspace, member, WorkspaceRole.MEMBER);

            // then
            assertThat(result).isEqualTo(existingMember);
            then(workspacePolicy).shouldHaveNoInteractions();
            then(memberPolicy).shouldHaveNoInteractions();
            then(workspaceMemberCommandRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("success: soft-deleted member is restored after capacity checks")
        void successSoftDeletedMemberRestored() {
            // given
            Workspace workspace = mock(Workspace.class);
            Member member = mock(Member.class);
            WorkspaceMember softDeletedMember = mock(WorkspaceMember.class);

            given(workspace.getKey()).willReturn("WORKSPACE");
            given(workspaceMemberFinder.getOptionalIncludingSoftDeleted(workspace, member))
                    .willReturn(Optional.of(softDeletedMember));
            given(softDeletedMember.isSoftDeleted()).willReturn(true);
            given(workspaceMemberFinder.countTotalMembersIncludingSoftDeleted("WORKSPACE"))
                    .willReturn(3);
            given(workspaceMemberFinder.countJoinedWorkspaces(member)).willReturn(1);

            // when
            WorkspaceMember result = sut.join(workspace, member, WorkspaceRole.MEMBER);

            // then
            assertThat(result).isEqualTo(softDeletedMember);
            then(softDeletedMember).should().restoreSoftDeleted();
            then(workspacePolicy).should().ensureCanAddMember(3);
            then(memberPolicy).should().ensureCanJoinWorkspace(1);
            then(workspaceMemberCommandRepository).should(never()).save(any());
        }

        @Test
        @DisplayName("fail: workspace capacity exceeded throws BadRequestException")
        void failJoin_If_WorkspaceLimitExceeded() {
            // given
            Workspace workspace = mock(Workspace.class);
            Member member = mock(Member.class);

            given(workspace.getKey()).willReturn("WORKSPACE");
            given(workspaceMemberFinder.getOptionalIncludingSoftDeleted(workspace, member))
                    .willReturn(Optional.empty());
            given(workspaceMemberFinder.countTotalMembersIncludingSoftDeleted("WORKSPACE"))
                    .willReturn(100);

            willThrow(new BadRequestException(mock(com.tissue.shared.exception.ErrorCode.class)))
                    .given(workspacePolicy)
                    .ensureCanAddMember(100);

            // when & then
            assertThatThrownBy(() -> sut.join(workspace, member, WorkspaceRole.MEMBER))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("fail: member join limit exceeded throws BadRequestException")
        void failJoin_If_MemberLimitExceeded() {
            // given
            Workspace workspace = mock(Workspace.class);
            Member member = mock(Member.class);

            given(workspace.getKey()).willReturn("WORKSPACE");
            given(workspaceMemberFinder.getOptionalIncludingSoftDeleted(workspace, member))
                    .willReturn(Optional.empty());
            given(workspaceMemberFinder.countTotalMembersIncludingSoftDeleted("WORKSPACE"))
                    .willReturn(5);
            given(workspaceMemberFinder.countJoinedWorkspaces(member)).willReturn(10);

            willThrow(new BadRequestException(mock(com.tissue.shared.exception.ErrorCode.class)))
                    .given(memberPolicy)
                    .ensureCanJoinWorkspace(10);

            // when & then
            assertThatThrownBy(() -> sut.join(workspace, member, WorkspaceRole.MEMBER))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
