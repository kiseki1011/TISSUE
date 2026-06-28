package com.tissue.feature.project.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.project.application.dto.response.ProjectMemberResponse;
import com.tissue.feature.project.application.dto.response.ProjectMembersResponse;
import com.tissue.feature.project.application.port.repository.ProjectMemberCommandRepository;
import com.tissue.feature.project.application.service.authorization.ProjectAuthorizationService;
import com.tissue.feature.project.application.service.finder.ProjectAccessResolver;
import com.tissue.feature.project.application.service.finder.ProjectFinder;
import com.tissue.feature.project.application.service.finder.ProjectMemberFinder;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.feature.project.domain.ProjectRole;
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectMemberServiceTest {

    @Mock
    private ProjectFinder projectFinder;

    @Mock
    private ProjectAccessResolver projectAccessResolver;

    @Mock
    private ProjectMemberFinder projectMemberFinder;

    @Mock
    private MemberFinder memberFinder;

    @Mock
    private ProjectMemberCommandRepository projectMemberRepository;

    @Mock
    private ProjectAuthorizationService projectAuthorizationService;

    @Mock
    private ProjectEventPublisher projectEventPublisher;

    @Mock
    private AgentProjectJoinService agentProjectJoinService;

    @InjectMocks
    private ProjectMemberService sut;

    @Nested
    @DisplayName("add members to project")
    class AddProjectMembers {

        @Test
        @DisplayName("success: creates members with no existing row and skips already-active members")
        void successAddOnlyNewMembers() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Set<Long> targetMemberIds = Set.of(2L, 3L, 4L);

            ProjectMember actor = mock(ProjectMember.class);
            Project project = mock(Project.class);
            Member m2 = mock(Member.class);
            Member m3 = mock(Member.class);
            Member m4 = mock(Member.class);
            ProjectMember activeMember = mock(ProjectMember.class);

            given(projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getAllActiveByIds(targetMemberIds)).willReturn(List.of(m2, m3, m4));

            given(m2.getId()).willReturn(2L);
            given(m3.getId()).willReturn(3L);
            given(m4.getId()).willReturn(4L);

            // 2 and 4 have no row -> created; 3 is already an active member -> skipped
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, 2L))
                    .willReturn(Optional.empty());
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, 4L))
                    .willReturn(Optional.empty());
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, 3L))
                    .willReturn(Optional.of(activeMember));
            given(activeMember.isSoftDeleted()).willReturn(false);

            given(project.getKey()).willReturn(projectKey);
            given(project.isArchived()).willReturn(false);

            // when
            ProjectMembersResponse result = sut.addMembers(pid, targetMemberIds, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            then(projectMemberRepository).should(times(2)).save(any(ProjectMember.class));
            assertThat(result.totalSize()).isEqualTo(2);
            assertThat(result.memberIds()).containsExactlyInAnyOrder(2L, 4L);
        }

        @Test
        @DisplayName("success: re-adds a previously kicked member by restoring the soft-deleted row")
        void successRestoresSoftDeletedMember() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Set<Long> targetMemberIds = Set.of(2L);

            ProjectMember actor = mock(ProjectMember.class);
            Project project = mock(Project.class);
            Member m2 = mock(Member.class);
            ProjectMember softDeleted = mock(ProjectMember.class);

            given(projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getAllActiveByIds(targetMemberIds)).willReturn(List.of(m2));
            given(m2.getId()).willReturn(2L);

            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, 2L))
                    .willReturn(Optional.of(softDeleted));
            given(softDeleted.isSoftDeleted()).willReturn(true);
            given(softDeleted.getMemberId()).willReturn(2L);

            given(project.getKey()).willReturn(projectKey);

            // when
            ProjectMembersResponse result = sut.addMembers(pid, targetMemberIds, actorMemberId);

            // then: the soft-deleted row is restored, not inserted as a duplicate
            then(softDeleted).should().restoreSoftDeleted();
            then(softDeleted).should().changeRole(ProjectRole.MEMBER);
            then(projectMemberRepository).should(never()).save(any());
            assertThat(result.memberIds()).containsExactly(2L);
        }

        @Test
        @DisplayName("success: all target members already active results in empty addition")
        void successAllMembersAlreadyExist() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Set<Long> targetMemberIds = Set.of(2L, 3L);

            ProjectMember actor = mock(ProjectMember.class);
            Project project = mock(Project.class);
            Member m2 = mock(Member.class);
            Member m3 = mock(Member.class);
            ProjectMember pm2 = mock(ProjectMember.class);
            ProjectMember pm3 = mock(ProjectMember.class);

            given(projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getAllActiveByIds(targetMemberIds)).willReturn(List.of(m2, m3));

            given(m2.getId()).willReturn(2L);
            given(m3.getId()).willReturn(3L);
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, 2L))
                    .willReturn(Optional.of(pm2));
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, 3L))
                    .willReturn(Optional.of(pm3));
            given(pm2.isSoftDeleted()).willReturn(false);
            given(pm3.isSoftDeleted()).willReturn(false);

            given(project.getKey()).willReturn(projectKey);

            // when
            ProjectMembersResponse result = sut.addMembers(pid, targetMemberIds, actorMemberId);

            // then
            then(projectMemberRepository).should(never()).save(any());
            assertThat(result.totalSize()).isEqualTo(0);
            assertThat(result.memberIds()).isEmpty();
        }
    }

    @Nested
    @DisplayName("join project")
    class JoinProject {

        @Test
        @DisplayName("success: new member joins and is saved")
        void successNewMemberJoins() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Project project = mock(Project.class);
            Member actor = mock(Member.class);

            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, actorMemberId))
                    .willReturn(Optional.empty());
            given(project.isArchived()).willReturn(false);
            given(project.getKey()).willReturn(projectKey);
            given(actor.getId()).willReturn(actorMemberId);

            // when
            ProjectMemberResponse result = sut.join(pid, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireJoinPermission(actor, project);
            then(projectMemberRepository).should().save(any(ProjectMember.class));
            assertThat(result.memberId()).isEqualTo(actorMemberId);
            assertThat(result.projectKey()).isEqualTo(projectKey);
        }

        @Test
        @DisplayName("success: existing active member returns without saving")
        void successExistingMemberReturnsEarly() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Project project = mock(Project.class);
            Member actor = mock(Member.class);
            ProjectMember activeMember = mock(ProjectMember.class);

            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, actorMemberId))
                    .willReturn(Optional.of(activeMember));
            given(activeMember.isSoftDeleted()).willReturn(false);
            given(activeMember.getProjectKey()).willReturn(projectKey);
            given(activeMember.getMemberId()).willReturn(actorMemberId);

            // when
            ProjectMemberResponse result = sut.join(pid, actorMemberId);

            // then: an already-active member is a no-op and is NOT join-permission
            // checked, so re-entering a PRIVATE project they belong to never fails.
            then(projectAuthorizationService).should(never()).requireJoinPermission(any(), any());
            then(projectMemberRepository).should(never()).save(any());
            assertThat(result.projectKey()).isEqualTo(projectKey);
            assertThat(result.memberId()).isEqualTo(actorMemberId);
        }

        @Test
        @DisplayName("success: re-joining after leaving restores the soft-deleted membership")
        void successSoftDeletedMemberRestored() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Project project = mock(Project.class);
            Member actor = mock(Member.class);
            ProjectMember softDeleted = mock(ProjectMember.class);

            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(projectMemberFinder.findOptionalIncludingSoftDeleted(project, actorMemberId))
                    .willReturn(Optional.of(softDeleted));
            given(softDeleted.isSoftDeleted()).willReturn(true);
            given(softDeleted.getProjectKey()).willReturn(projectKey);
            given(softDeleted.getMemberId()).willReturn(actorMemberId);

            // when
            ProjectMemberResponse result = sut.join(pid, actorMemberId);

            // then: re-joining (restoring) IS gated by join permission, then the
            // soft-deleted membership is restored, not inserted as a duplicate
            then(projectAuthorizationService).should().requireJoinPermission(actor, project);
            then(softDeleted).should().restoreSoftDeleted();
            then(softDeleted).should().changeRole(ProjectRole.MEMBER);
            then(agentProjectJoinService).should().includeAgentsOfMember(actorMemberId, project);
            then(projectMemberRepository).should(never()).save(any());
            assertThat(result.memberId()).isEqualTo(actorMemberId);
            assertThat(result.projectKey()).isEqualTo(projectKey);
        }
    }

    @Nested
    @DisplayName("kick member from project")
    class KickProjectMember {

        @Test
        @DisplayName("success: kicks target member after authorization")
        void successKickMember() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long targetMemberId = 2L;
            Long actorMemberId = 1L;
            ProjectMember actor = mock(ProjectMember.class);
            ProjectMember target = mock(ProjectMember.class);

            given(projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId))
                    .willReturn(actor);
            given(projectMemberFinder.getWithProject(projectKey, targetMemberId))
                    .willReturn(target);

            // when
            sut.kickMember(pid, targetMemberId, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireHigherRole(actor, target);
            then(target).should().softDelete();
        }

        @Test
        @DisplayName("fail: self-kick throws BadRequestException")
        void failSelfKick() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("PROJ");
            Long actorMemberId = 1L;
            ProjectMember actor = mock(ProjectMember.class);

            given(projectAccessResolver.resolveByProjectKey("PROJ", actorMemberId))
                    .willReturn(actor);

            // when & then
            assertThatThrownBy(() -> sut.kickMember(pid, actorMemberId, actorMemberId))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("leave project")
    class LeaveProject {

        @Test
        @DisplayName("success: leaves project by soft delete")
        void successLeaveProject() {
            // given
            ProjectIdentifier pid = new ProjectIdentifier("PROJ");
            Long actorMemberId = 1L;
            ProjectMember actor = mock(ProjectMember.class);

            given(projectMemberFinder.getWithProject("PROJ", actorMemberId)).willReturn(actor);

            // when
            sut.leave(pid, actorMemberId);

            // then
            then(actor).should().softDelete();
        }
    }
}
