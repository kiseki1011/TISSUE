package com.tissue.feature.project.application.service.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.project.domain.Project;
import com.tissue.feature.project.domain.ProjectMember;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProjectAuthorizationServiceTest {

    private final ProjectAuthorizationService sut = new ProjectAuthorizationService();

    private ProjectMember actor(SystemRole systemRole, boolean manager) {
        Member member = mock(Member.class);
        given(member.hasAtLeast(SystemRole.ADMIN)).willReturn(systemRole.isEqualOrHigherThan(SystemRole.ADMIN));
        ProjectMember pm = mock(ProjectMember.class);
        given(pm.getMember()).willReturn(member);
        given(pm.isManager()).willReturn(manager);
        return pm;
    }

    private Member member(SystemRole systemRole) {
        Member member = mock(Member.class);
        given(member.hasAtLeast(SystemRole.ADMIN)).willReturn(systemRole.isEqualOrHigherThan(SystemRole.ADMIN));
        return member;
    }

    @Nested
    @DisplayName("requireProjectManager")
    class RequireProjectManager {

        @Test
        @DisplayName("system ADMIN/SUPER_ADMIN overrides and passes")
        void systemAdminPasses() {
            assertThatCode(() -> sut.requireProjectManager(actor(SystemRole.ADMIN, false)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("project MANAGER passes")
        void managerPasses() {
            assertThatCode(() -> sut.requireProjectManager(actor(SystemRole.USER, true)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("plain member without manager role is rejected")
        void plainMemberRejected() {
            assertThatThrownBy(() -> sut.requireProjectManager(actor(SystemRole.USER, false)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requireSystemAdmin")
    class RequireSystemAdmin {

        @Test
        @DisplayName("system ADMIN passes")
        void adminPasses() {
            assertThatCode(() -> sut.requireSystemAdmin(actor(SystemRole.ADMIN, false)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a non-admin (even a project manager) is rejected")
        void nonAdminRejected() {
            assertThatThrownBy(() -> sut.requireSystemAdmin(actor(SystemRole.USER, true)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requireJoinPermission")
    class RequireJoinPermission {

        @Test
        @DisplayName("system admin can join any project")
        void systemAdminJoinsAny() {
            Project project = mock(Project.class);
            assertThatCode(() -> sut.requireJoinPermission(member(SystemRole.ADMIN), project))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a user can join a public project")
        void userJoinsPublic() {
            Project project = mock(Project.class);
            given(project.isPublic()).willReturn(true);
            assertThatCode(() -> sut.requireJoinPermission(member(SystemRole.USER), project))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a user cannot join a private project")
        void userCannotJoinPrivate() {
            Project project = mock(Project.class);
            given(project.isPublic()).willReturn(false);
            assertThatThrownBy(() -> sut.requireJoinPermission(member(SystemRole.USER), project))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("requireHigherRole")
    class RequireHigherRole {

        @Test
        @DisplayName("system admin passes regardless of target")
        void systemAdminPasses() {
            assertThatCode(() -> sut.requireHigherRole(actor(SystemRole.ADMIN, false), actor(SystemRole.USER, true)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a manager can act on a non-manager")
        void managerOverNonManager() {
            assertThatCode(() -> sut.requireHigherRole(actor(SystemRole.USER, true), actor(SystemRole.USER, false)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a manager cannot act on another manager")
        void managerOverManagerRejected() {
            assertThatThrownBy(() -> sut.requireHigherRole(actor(SystemRole.USER, true), actor(SystemRole.USER, true)))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("a non-manager cannot act on anyone")
        void nonManagerRejected() {
            assertThatThrownBy(
                            () -> sut.requireHigherRole(actor(SystemRole.USER, false), actor(SystemRole.USER, false)))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
