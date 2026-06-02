package com.tissue.admin.application.service;

import com.tissue.admin.application.dto.AdminSystemInfo;
import com.tissue.admin.application.port.usecase.AdminSystemInfoUseCase;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.security.config.SystemProperties;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.ACCEPTABLE,
        evaluationReason = "Passes human written integration test.",
        agentName = "claude-opus-4-8",
        reviewedBy = "kiseki1011")
@Service
@RequiredArgsConstructor
public class AdminSystemInfoService implements AdminSystemInfoUseCase {

    private final SystemProperties systemProperties;
    private final MemberQueryRepository memberQueryRepository;
    private final Environment environment;

    @Override
    @Transactional(readOnly = true)
    public AdminSystemInfo getSystemInfo() {
        long total = memberQueryRepository.count();
        boolean redisEnabled = environment.getProperty("tissue.use-redis", Boolean.class, false);

        AdminSystemInfo.MemberStats stats = AdminSystemInfo.MemberStats.builder()
                .total(total)
                .active(memberQueryRepository.countByStatus(MemberStatus.ACTIVE))
                .locked(memberQueryRepository.countByStatus(MemberStatus.LOCKED))
                .deleted(memberQueryRepository.countByStatus(MemberStatus.DELETED))
                .purged(memberQueryRepository.countByStatus(MemberStatus.PURGED))
                .activeSuperAdmins(
                        memberQueryRepository.countByRoleAndStatus(SystemRole.SUPER_ADMIN, MemberStatus.ACTIVE))
                .build();

        return AdminSystemInfo.builder()
                .version(systemProperties.getVersion())
                .serverName(systemProperties.getServerName())
                .activeProfiles(List.of(environment.getActiveProfiles()))
                .redisEnabled(redisEnabled)
                .seeded(total > 0)
                .members(stats)
                .build();
    }
}
