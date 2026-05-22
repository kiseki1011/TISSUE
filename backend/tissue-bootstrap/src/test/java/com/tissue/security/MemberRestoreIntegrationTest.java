package com.tissue.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.config.MemberDeletionProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.service.MemberAccountService;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.exception.AuthenticationErrorCode;
import com.tissue.shared.exception.TissueException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.exception.base.UnauthorizedException;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberRestoreIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberAccountService memberAccountService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MemberDeletionProperties properties;

    @AfterEach
    void tearDown() {
        properties.setRetention(Duration.ofDays(7));
    }

    @Test
    @DisplayName("restores a DELETED member back to ACTIVE")
    void restoresWithinRetention() {
        // given — withdrawn: 2 days ago, retention: 7 days
        properties.setRetention(Duration.ofDays(7));
        Member member = createWithdrawnMember(
                "gildong@tissue.com", "password1234!", Instant.now().minus(Duration.ofDays(2)));

        // when
        memberAccountService.restore("gildong@tissue.com", "password1234!");
        em.flush();
        em.clear();

        // then
        Member after = memberQueryRepository.findById(member.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(after.getDeletedAt()).isNull();
    }

    @Test
    @DisplayName("RESTORE_INVALID_CREDENTIALS when no auth identity matches the identifier")
    void invalidCredentialsWhenIdentifierUnknown() {
        assertThatThrownBy(() -> memberAccountService.restore("nobody@tissue.com", "password1234!"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((TissueException) e).getErrorCode())
                .isEqualTo(AuthenticationErrorCode.RESTORE_INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("RESTORE_INVALID_CREDENTIALS when password does not match")
    void invalidCredentials() {
        // given
        properties.setRetention(Duration.ofDays(7));
        createWithdrawnMember(
                "gildong@tissue.com", "password1234!", Instant.now().minus(Duration.ofDays(2)));

        // when & then
        assertThatThrownBy(() -> memberAccountService.restore("gildong@tissue.com", "wrongPassword"))
                .isInstanceOf(UnauthorizedException.class)
                .extracting(e -> ((TissueException) e).getErrorCode())
                .isEqualTo(AuthenticationErrorCode.RESTORE_INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("RESTORE_NOT_DELETED when the member is still ACTIVE")
    void notDeletedWhenActive() {
        // given
        Member member = memberCommandRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));
        authenticationIdentityRepository.save(AuthenticationIdentity.createEmailIdentity(
                member, "gildong@tissue.com", passwordEncoder.encode("password1234!")));
        em.flush();
        em.clear();

        // when & then
        assertThatThrownBy(() -> memberAccountService.restore("gildong@tissue.com", "password1234!"))
                .isInstanceOf(ResourceConflictException.class)
                .extracting(e -> ((TissueException) e).getErrorCode())
                .isEqualTo(AuthenticationErrorCode.RESTORE_NOT_DELETED);
    }

    /**
     * Saves an ACTIVE member with an EMAIL auth identity, withdraw it, and
     * sets a specific deletedAt.
     */
    private Member createWithdrawnMember(String email, String rawPassword, Instant withdrawnAt) {
        Member member = memberCommandRepository.save(Member.create(email, email.split("@")[0], "Test User"));
        authenticationIdentityRepository.save(
                AuthenticationIdentity.createEmailIdentity(member, email, passwordEncoder.encode(rawPassword)));
        member.withdraw();
        memberCommandRepository.save(member);
        em.flush();

        em.createQuery("UPDATE Member m SET m.deletedAt = :ts WHERE m.id = :id")
                .setParameter("ts", withdrawnAt)
                .setParameter("id", member.getId())
                .executeUpdate();
        em.flush();
        em.clear();

        return memberQueryRepository.findById(member.getId()).orElseThrow();
    }
}
