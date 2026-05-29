package com.tissue.security.application.service;

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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberAccountValidatorTest {

    private final MemberQueryRepository memberRepository = mock(MemberQueryRepository.class);
    private final MemberAccountValidator sut = new MemberAccountValidator(memberRepository);

    @Nested
    @DisplayName("ensureWithdrawable")
    class EnsureWithdrawable {

        @Test
        @DisplayName("a non-super-admin can always withdraw")
        void nonSuperAdminCanWithdraw() {
            Member member = mock(Member.class);
            given(member.isSuperAdmin()).willReturn(false);

            assertThatCode(() -> sut.ensureWithdrawable(member)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("a super-admin can withdraw when another active super-admin remains")
        void superAdminCanWithdrawWhenNotLast() {
            Member member = mock(Member.class);
            given(member.isSuperAdmin()).willReturn(true);
            given(memberRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                    .willReturn(2L);

            assertThatCode(() -> sut.ensureWithdrawable(member)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the last active super-admin cannot withdraw")
        void lastSuperAdminCannotWithdraw() {
            Member member = mock(Member.class);
            given(member.isSuperAdmin()).willReturn(true);
            given(memberRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                    .willReturn(1L);

            assertThatThrownBy(() -> sut.ensureWithdrawable(member)).isInstanceOf(LastSuperAdminException.class);
        }
    }
}
