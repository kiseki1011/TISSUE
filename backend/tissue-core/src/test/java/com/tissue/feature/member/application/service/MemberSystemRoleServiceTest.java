package com.tissue.feature.member.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.CannotDemoteSelfSuperAdminException;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import com.tissue.shared.exception.base.ForbiddenException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberSystemRoleServiceTest {

    private final MemberFinder memberFinder = mock(MemberFinder.class);
    private final MemberQueryRepository memberQueryRepository = mock(MemberQueryRepository.class);
    private final MemberSystemRoleService sut = new MemberSystemRoleService(memberFinder, memberQueryRepository);

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
        @DisplayName("success: demotes a SUPER_ADMIN to ADMIN when another active SUPER_ADMIN remains")
        void demotesWhenNotLastSuperAdmin() {
            // given
            Member target = superAdmin();
            given(memberFinder.getActiveById(1L)).willReturn(admin());
            given(memberFinder.getActiveById(2L)).willReturn(target);
            given(memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                    .willReturn(2L);

            // when
            sut.changeSystemRole(1L, 2L, SystemRole.ADMIN);

            // then
            assertThat(target.getRole()).isEqualTo(SystemRole.ADMIN);
        }

        @Test
        @DisplayName("success: promotes a USER to SUPER_ADMIN without a super-admin count check")
        void promotesUserToSuperAdmin() {
            // given
            Member target = user();
            given(memberFinder.getActiveById(1L)).willReturn(admin());
            given(memberFinder.getActiveById(2L)).willReturn(target);

            // when
            sut.changeSystemRole(1L, 2L, SystemRole.SUPER_ADMIN);

            // then
            assertThat(target.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("success: changing a non-SUPER_ADMIN role does not trigger super-admin guards")
        void changingNonSuperAdminRole() {
            // given
            Member target = user();
            given(memberFinder.getActiveById(1L)).willReturn(admin());
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
        @DisplayName("fail: the last active SUPER_ADMIN cannot be demoted")
        void rejectsLastSuperAdminDemotion() {
            // given
            Member target = superAdmin();
            given(memberFinder.getActiveById(1L)).willReturn(admin());
            given(memberFinder.getActiveById(2L)).willReturn(target);
            given(memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                    .willReturn(1L);

            // when & then
            assertThatThrownBy(() -> sut.changeSystemRole(1L, 2L, SystemRole.ADMIN))
                    .isInstanceOf(LastSuperAdminException.class);
            assertThat(target.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
        }

        @Test
        @DisplayName("fail: a non-admin actor cannot change roles")
        void rejectsNonAdminActor() {
            // given
            given(memberFinder.getActiveById(1L)).willReturn(user());

            // when & then
            assertThatThrownBy(() -> sut.changeSystemRole(1L, 2L, SystemRole.ADMIN))
                    .isInstanceOf(ForbiddenException.class);
        }
    }
}
