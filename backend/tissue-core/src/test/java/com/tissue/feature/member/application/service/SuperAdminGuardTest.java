package com.tissue.feature.member.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuperAdminGuardTest {

    private final MemberQueryRepository memberQueryRepository = mock(MemberQueryRepository.class);
    private final SuperAdminGuard sut = new SuperAdminGuard(memberQueryRepository);

    @Test
    @DisplayName("a non SUPER_ADMIN is always allowed")
    void nonSuperAdminAllowed() {
        Member member = mock(Member.class);
        given(member.isSuperAdmin()).willReturn(false);

        assertThatCode(() -> sut.ensureNotLastActiveSuperAdmin(member)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("a SUPER_ADMIN is allowed when another active SUPER_ADMIN remains")
    void superAdminAllowedWhenNotLast() {
        Member member = mock(Member.class);
        given(member.isSuperAdmin()).willReturn(true);
        given(memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                .willReturn(2L);

        assertThatCode(() -> sut.ensureNotLastActiveSuperAdmin(member)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("the last active SUPER_ADMIN is rejected")
    void lastSuperAdminRejected() {
        Member member = mock(Member.class);
        given(member.isSuperAdmin()).willReturn(true);
        given(memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                .willReturn(1L);

        assertThatThrownBy(() -> sut.ensureNotLastActiveSuperAdmin(member)).isInstanceOf(LastSuperAdminException.class);
    }
}
