package com.tissue.feature.workspace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.policy.MemberPolicy;
import com.tissue.feature.workspace.application.dto.request.CreateWorkspaceCommand;
import com.tissue.feature.workspace.application.dto.response.command.WorkspaceCreateResponse;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberCommandRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceMemberQueryRepository;
import com.tissue.feature.workspace.application.port.repository.WorkspaceRepository;
import com.tissue.feature.workspace.application.service.authorization.WorkspaceAuthorizationService;
import com.tissue.feature.workspace.application.service.finder.WorkspaceFinder;
import com.tissue.feature.workspace.application.service.finder.WorkspaceMemberFinder;
import com.tissue.feature.workspace.application.service.publisher.WorkspaceEventPublisher;
import com.tissue.feature.workspace.domain.Workspace;
import com.tissue.feature.workspace.domain.WorkspaceMember;
import com.tissue.feature.workspace.domain.exception.DuplicateWorkspaceKeyException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private MemberFinder memberFinder;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberCommandRepository workspaceMemberCommandRepository;

    @Mock
    private WorkspaceMemberFinder workspaceMemberFinder;

    @Mock
    private MemberPolicy memberPolicy;

    @Mock
    private WorkspaceFinder workspaceFinder;

    @Mock
    private WorkspaceAuthorizationService workspaceAuthorizationService;

    @Mock
    private WorkspaceMemberQueryRepository workspaceMemberQueryRepository;

    @Mock
    private WorkspaceEventPublisher workspaceEventPublisher;

    @InjectMocks
    private WorkspaceService sut;

    @Nested
    @DisplayName("create workspace")
    class CreateWorkspace {

        @Test
        @DisplayName("success: creates workspace and owner member")
        void successCreateWorkspace() {
            // given
            Long actorMemberId = 1L;
            CreateWorkspaceCommand cmd = new CreateWorkspaceCommand("WORKSPACE", "Test workspace", "desc");
            Member member = mock(Member.class);
            Workspace savedWorkspace = mock(Workspace.class);

            given(memberFinder.getActiveBy(actorMemberId)).willReturn(member);
            given(workspaceRepository.existsByKey("WORKSPACE")).willReturn(false);
            given(workspaceMemberFinder.countOwnedWorkspaces(member)).willReturn(0);
            given(workspaceMemberFinder.countJoinedWorkspaces(member)).willReturn(1);
            given(workspaceRepository.save(any(Workspace.class))).willReturn(savedWorkspace);
            given(savedWorkspace.getKey()).willReturn("WORKSPACE");

            // when
            WorkspaceCreateResponse result = sut.create(cmd, actorMemberId);

            // then
            assertThat(result.workspaceKey()).isEqualTo("WORKSPACE");
            then(memberPolicy).should().ensureCanCreateWorkspace(0, 1);
            then(workspaceMemberCommandRepository).should().save(any(WorkspaceMember.class));
        }

        @Test
        @DisplayName("fail: duplicate key detected throws DuplicateWorkspaceKeyException")
        void failCreateWorkspace_If_DuplicateKey() {
            // given
            Long actorMemberId = 1L;
            CreateWorkspaceCommand cmd = new CreateWorkspaceCommand("DUPE", "Duplicate", null);
            Member member = mock(Member.class);

            given(memberFinder.getActiveBy(actorMemberId)).willReturn(member);
            given(workspaceRepository.existsByKey("DUPE")).willReturn(true);

            // when & then
            assertThatThrownBy(() -> sut.create(cmd, actorMemberId)).isInstanceOf(DuplicateWorkspaceKeyException.class);
        }

        @Test
        @DisplayName("fail: DataIntegrityViolation -> DuplicateWorkspaceKeyException")
        void failDataIntegrityViolation() {
            // given
            Long actorMemberId = 1L;
            CreateWorkspaceCommand cmd = new CreateWorkspaceCommand("RACEWORKSPACE", "Race workspace", null);
            Member member = mock(Member.class);

            given(memberFinder.getActiveBy(actorMemberId)).willReturn(member);
            given(workspaceRepository.existsByKey("RACEWORKSPACE")).willReturn(false);
            given(workspaceMemberFinder.countOwnedWorkspaces(member)).willReturn(0);
            given(workspaceMemberFinder.countJoinedWorkspaces(member)).willReturn(0);
            given(workspaceRepository.save(any(Workspace.class)))
                    .willThrow(new DataIntegrityViolationException("duplicate key"));

            // when & then
            assertThatThrownBy(() -> sut.create(cmd, actorMemberId)).isInstanceOf(DuplicateWorkspaceKeyException.class);
        }
    }

    @Nested
    @DisplayName("delete workspace")
    class DeleteWorkspace {

        @Test
        @DisplayName("success: soft deletes workspace and publishes event")
        void successDeleteWorkspace() {
            // given
            String workspaceKey = "WORKSPACE";
            Long actorMemberId = 1L;
            WorkspaceMember actor = mock(WorkspaceMember.class);
            Workspace workspace = mock(Workspace.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(actor);
            given(actor.getWorkspace()).willReturn(workspace);

            // when
            sut.delete(workspaceKey, actorMemberId);

            // then
            then(workspaceAuthorizationService).should().requireWorkspaceOwner(actor);
            then(workspace).should().softDelete();
            then(workspaceEventPublisher).should().publishWorkspaceDeleted(workspace, actor);
        }
    }

    @Nested
    @DisplayName("transfer workspace ownership")
    class TransferWorkspaceOwnership {

        @Test
        @DisplayName("success: transfers ownership and publishes event")
        void successTransferOwnership() {
            // given
            String workspaceKey = "WORKSPACE";
            Long targetMemberId = 2L;
            Long actorMemberId = 1L;
            WorkspaceMember originalOwner = mock(WorkspaceMember.class);
            WorkspaceMember newOwner = mock(WorkspaceMember.class);
            Workspace workspace = mock(Workspace.class);

            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, actorMemberId))
                    .willReturn(originalOwner);
            given(workspaceMemberFinder.getWithWorkspace(workspaceKey, targetMemberId))
                    .willReturn(newOwner);
            given(originalOwner.getWorkspace()).willReturn(workspace);

            // when
            sut.transferOwnership(workspaceKey, targetMemberId, actorMemberId);

            // then
            then(workspaceAuthorizationService).should().requireWorkspaceOwner(originalOwner);
            then(workspace).should().transferOwnership(originalOwner, newOwner);
            then(workspaceEventPublisher).should().publishOwnershipTransferred(workspaceKey, newOwner, originalOwner);
        }
    }
}
