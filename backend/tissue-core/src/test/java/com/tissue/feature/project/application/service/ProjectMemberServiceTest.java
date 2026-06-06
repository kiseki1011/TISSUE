package com.tissue.feature.project.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

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
import com.tissue.shared.dto.ProjectIdentifier;
import com.tissue.shared.exception.base.BadRequestException;
import java.util.List;
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
        @DisplayName("success: adds only non-existing members to project")
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

            given(projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getAllActiveByIds(targetMemberIds)).willReturn(List.of(m2, m3, m4));
            given(projectMemberFinder.getExistingMemberIds(project, targetMemberIds))
                    .willReturn(Set.of(3L));

            given(m2.getId()).willReturn(2L);
            given(m3.getId()).willReturn(3L);
            given(m4.getId()).willReturn(4L);

            given(project.getKey()).willReturn(projectKey);
            given(project.isArchived()).willReturn(false);

            // when
            ProjectMembersResponse result = sut.addMembers(pid, targetMemberIds, actorMemberId);

            // then
            then(projectAuthorizationService).should().requireProjectManager(actor);
            assertThat(result.totalSize()).isEqualTo(2);
            assertThat(result.memberIds()).containsExactlyInAnyOrder(2L, 4L);
        }

        @Test
        @DisplayName("success: all target members already exist results in empty addition")
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

            given(projectAccessResolver.resolveByProjectKey(projectKey, actorMemberId))
                    .willReturn(actor);
            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getAllActiveByIds(targetMemberIds)).willReturn(List.of(m2, m3));
            given(projectMemberFinder.getExistingMemberIds(project, targetMemberIds))
                    .willReturn(Set.of(2L, 3L));

            given(m2.getId()).willReturn(2L);
            given(m3.getId()).willReturn(3L);

            given(project.getKey()).willReturn(projectKey);

            // when
            ProjectMembersResponse result = sut.addMembers(pid, targetMemberIds, actorMemberId);

            // then
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
            given(projectMemberFinder.existsByIncludingSoftDeleted(project, actorMemberId))
                    .willReturn(false);
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
        @DisplayName("success: existing member returns early without saving")
        void successExistingMemberReturnsEarly() {
            // given
            String projectKey = "PROJ";
            ProjectIdentifier pid = new ProjectIdentifier(projectKey);
            Long actorMemberId = 1L;
            Project project = mock(Project.class);
            Member actor = mock(Member.class);

            given(projectFinder.getByProjectKey(projectKey)).willReturn(project);
            given(memberFinder.getActiveById(actorMemberId)).willReturn(actor);
            given(projectMemberFinder.existsByIncludingSoftDeleted(project, actorMemberId))
                    .willReturn(true);

            // when
            ProjectMemberResponse result = sut.join(pid, actorMemberId);

            // then
            then(projectMemberRepository).should(never()).save(any());
            assertThat(result.projectKey()).isEqualTo(projectKey);
            assertThat(result.memberId()).isEqualTo(actorMemberId);
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
