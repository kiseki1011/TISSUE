package com.tissue.feature.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.config.MemberDeletionProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.security.adapter.scheduler.MemberAnonymizationScheduler;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.domain.AuthenticationIdentity;
import com.tissue.security.domain.AuthenticationIdentityProvider;
import com.tissue.support.IntegrationTestSupport;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class MemberAnonymizationSchedulerIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private MemberAnonymizationScheduler scheduler;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Autowired
    private AuthenticationIdentityRepository authenticationIdentityRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private MemberDeletionProperties properties;

    @AfterEach
    void tearDown() {
        properties.setRetention(Duration.ofDays(7));
    }

    @Test
    @DisplayName("anonymizes (PII wipeout) members whose retention period has passed")
    void anonymizesPastRetention() {
        // given - withdrawn: 8 days ago, retention: 7 days
        properties.setRetention(Duration.ofDays(7));
        Member member = createWithdrawnMember(
                "gildong@tissue.com", "gildong", Instant.now().minus(Duration.ofDays(8)));

        // when
        scheduler.anonymizeExpiredDeletedMembers();

        // then
        Member after = memberQueryRepository.findById(member.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.PURGED);
        assertThat(after.getEmail()).isNull();
        assertThat(after.getName()).isEqualTo("Deleted User");
        assertThat(after.getUsername()).isEqualTo("deleted_" + member.getId());
    }

    @Test
    @DisplayName("leaves members still within retention untouched")
    void skipsWithinRetention() {
        // given — withdrawn: 2 days ago, retention: 7 days
        properties.setRetention(Duration.ofDays(7));
        Member member = createWithdrawnMember(
                "gildong@tissue.com", "gildong", Instant.now().minus(Duration.ofDays(2)));

        // when
        scheduler.anonymizeExpiredDeletedMembers();

        // then
        Member after = memberQueryRepository.findById(member.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.DELETED);
        assertThat(after.getEmail()).isEqualTo("gildong@tissue.com");
        assertThat(after.getUsername()).isEqualTo("gildong");
    }

    @Test
    @DisplayName("does not touch ACTIVE members")
    void skipsActiveMembers() {
        // given
        Member member = memberCommandRepository.save(Member.create("gildong@tissue.com", "gildong", "Hong Gildong"));

        // when
        scheduler.anonymizeExpiredDeletedMembers();

        // then
        Member after = memberQueryRepository.findById(member.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(after.getEmail()).isEqualTo("gildong@tissue.com");
    }

    @Test
    @DisplayName("deletes anonymization target member's authentication identities")
    void wipesAuthIdentities() {
        // given — member with an EMAIL auth identity, withdrawn past retention
        properties.setRetention(Duration.ofDays(7));
        Member member = createWithdrawnMember(
                "gildong@tissue.com", "gildong", Instant.now().minus(Duration.ofDays(8)));
        authenticationIdentityRepository.save(
                AuthenticationIdentity.createEmailIdentity(member, "gildong@tissue.com", "encoded-pw"));
        em.flush();
        em.clear();

        assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                        AuthenticationIdentityProvider.EMAIL, "gildong@tissue.com"))
                .isPresent();

        // when
        scheduler.anonymizeExpiredDeletedMembers();
        em.flush();
        em.clear();

        // then
        assertThat(authenticationIdentityRepository.findByProviderAndIdentifier(
                        AuthenticationIdentityProvider.EMAIL, "gildong@tissue.com"))
                .isEmpty();
    }

    @Test
    @DisplayName("uses retention period from properties")
    void honorsConfigurableRetention() {
        // given — withdrawn: 2 hours ago, retention: 1 hour
        properties.setRetention(Duration.ofHours(1));
        Member member = createWithdrawnMember(
                "gildong@tissue.com", "gildong", Instant.now().minus(Duration.ofHours(2)));

        // when
        scheduler.anonymizeExpiredDeletedMembers();

        // then
        Member after = memberQueryRepository.findById(member.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(MemberStatus.PURGED);
    }

    /**
     * Saves an ACTIVE member, withdraws it, then sets specific deletedAt
     */
    private Member createWithdrawnMember(String email, String username, Instant deletedAt) {
        Member member = memberCommandRepository.save(Member.create(email, username, "Test User"));
        member.withdraw();
        memberCommandRepository.save(member);
        em.flush();

        em.createQuery("UPDATE Member m SET m.deletedAt = :ts WHERE m.id = :id")
                .setParameter("ts", deletedAt)
                .setParameter("id", member.getId())
                .executeUpdate();
        em.flush();
        em.clear();

        return memberQueryRepository.findById(member.getId()).orElseThrow();
    }
}
