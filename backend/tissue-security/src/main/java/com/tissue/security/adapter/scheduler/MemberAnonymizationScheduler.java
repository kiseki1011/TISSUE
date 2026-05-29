package com.tissue.security.adapter.scheduler;

import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.config.MemberDeletionProperties;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.security.application.port.repository.AuthenticationIdentityRepository;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Schedular that wipes out PII from members whose retention window (since
 * withdrawal) has passed. The member row stays so data that
 * reference it (project memberships, issues/comments, etc.) keep a
 * stable FK target.
 *
 * <p>Cron and retention are configurable via {@link MemberDeletionProperties}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberAnonymizationScheduler {

    private final MemberQueryRepository memberQueryRepository;
    private final MemberCommandRepository memberCommandRepository;
    private final AuthenticationIdentityRepository authenticationIdentityRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final MemberDeletionProperties properties;

    @Transactional
    @Scheduled(cron = "${tissue.member.deletion.cleanup-cron:0 0 3 * * *}")
    public void anonymizeExpiredDeletedMembers() {
        Instant cutoff = Instant.now().minus(properties.getRetention());

        List<Member> candidates = memberQueryRepository.findAllByStatusAndDeletedAtBefore(MemberStatus.DELETED, cutoff);

        if (candidates.isEmpty()) {
            return;
        }

        log.info("Anonymizing {} member(s) past retention (cutoff={})", candidates.size(), cutoff);

        for (Member member : candidates) {
            Long memberId = member.getId();
            authenticationIdentityRepository.deleteByMemberId(memberId);
            refreshTokenRepository.deleteByMemberId(memberId);

            member.anonymize();
            memberCommandRepository.save(member);
        }

        log.info("Member anonymization sweep complete");
    }
}
