package com.tissue.admin.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SuperAdminGuard;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.CannotDemoteSelfSuperAdminException;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberSystemRoleServiceTest {

    private final MemberFinder memberFinder = mock(MemberFinder.class);
    private final SuperAdminGuard superAdminGuard = mock(SuperAdminGuard.class);
    private final MemberSystemRoleService sut = new MemberSystemRoleService(memberFinder, superAdminGuard);

    private Member superAdmin() {
        return Member.createAsSuperAdmin("super@tissue.com", "super", "Super Admin");
    }

    private Member user() {
        return Member.create("user@tissue.com", "user", "User");
    }

    private Member admin() {
        return Member.createAsAdmin("admin@tissue.com", "admin", "Admin");
    }

    @Nested
    @DisplayName("changeSystemRole()")
    class ChangeSystemRole {

        @Test
        @DisplayName("success: demotes a SUPER_ADMIN to ADMIN when the guard passes")
        void demotesWhenGuardPasses() {
            // given
            Member target = superAdmin();
            given(memberFinder.getActiveById(1L)).willReturn(superAdmin());
            given(memberFinder.getActiveById(2L)).willReturn(target);

            // when
            sut.changeSystemRole(1L, 2L, SystemRole.ADMIN);

            // then
            assertThat(target.getRole()).isEqualTo(SystemRole.ADMIN);
        }

        @Test
        @DisplayName("success: promotes a USER to SUPER_ADMIN (no demotion guard)")
        void promotesUserToSuperAdmin() {
            // given
            Member target = user();
            given(memberFinder.getActiveById(1L)).willReturn(superAdmin());
            given(memberFinder.getActiveById(2L)).willReturn(target);

            // when
            sut.changeSystemRole(1L, 2L, SystemRole.SUPER_ADMIN);

            // then
            assertThat(target.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("success: changing a non-SUPER_ADMIN role does not trigger the demotion guard")
        void changingNonSuperAdminRole() {
            // given
            Member target = user();
            given(memberFinder.getActiveById(1L)).willReturn(superAdmin());
            given(memberFinder.getActiveById(2L)).willReturn(target);

            // when & then
            assertThatCode(() -> sut.changeSystemRole(1L, 2L, SystemRole.ADMIN)).doesNotThrowAnyException();
            assertThat(target.getRole()).isEqualTo(SystemRole.ADMIN);
        }

        @Test
        @DisplayName("fail: a SUPER_ADMIN cannot demote themselves")
        void rejectsSelfDemotion() {
            // given
            Member target = superAdmin();
            given(memberFinder.getActiveById(1L)).willReturn(target);

            // when & then
            assertThatThrownBy(() -> sut.changeSystemRole(1L, 1L, SystemRole.ADMIN))
                    .isInstanceOf(CannotDemoteSelfSuperAdminException.class);
            assertThat(target.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("fail: demotion is rejected when the guard reports the last active SUPER_ADMIN")
        void rejectsLastSuperAdminDemotion() {
            // given
            Member target = superAdmin();
            given(memberFinder.getActiveById(1L)).willReturn(superAdmin());
            given(memberFinder.getActiveById(2L)).willReturn(target);
            willThrow(new LastSuperAdminException()).given(superAdminGuard).ensureNotLastActiveSuperAdmin(target);

            // when & then
            assertThatThrownBy(() -> sut.changeSystemRole(1L, 2L, SystemRole.ADMIN))
                    .isInstanceOf(LastSuperAdminException.class);
            assertThat(target.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("fail: a USER actor cannot change roles")
        void rejectsUserActor() {
            // given
            given(memberFinder.getActiveById(1L)).willReturn(user());

            // when & then
            assertThatThrownBy(() -> sut.changeSystemRole(1L, 2L, SystemRole.ADMIN))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        @DisplayName("fail: an ADMIN actor cannot change roles (SUPER_ADMIN only)")
        void rejectsAdminActor() {
            // given
            given(memberFinder.getActiveById(1L)).willReturn(admin());

            // when & then
            assertThatThrownBy(() -> sut.changeSystemRole(1L, 2L, SystemRole.ADMIN))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
