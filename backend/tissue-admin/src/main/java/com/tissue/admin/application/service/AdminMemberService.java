package com.tissue.admin.application.service;

import com.tissue.admin.adapter.persistence.MemberSearchSpecs;
import com.tissue.admin.application.dto.AdminMemberDetail;
import com.tissue.admin.application.dto.AdminMemberSummary;
import com.tissue.admin.application.port.usecase.AdminMemberUseCase;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SuperAdminGuard;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.member.domain.exception.MemberNotFoundException;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@LLMGenerated(
        llmInvolvement = LLMInvolvement.VIBE_CODED,
        evaluation = Evaluation.APPROVED,
        evaluationReason = "Passes integration test which was human written. Code was reviewed.",
        agentName = "claude-opus-4-8",
        reviewedBy = "kiseki1011")
@Service
@Transactional
@RequiredArgsConstructor
public class AdminMemberService implements AdminMemberUseCase {

    private final MemberFinder memberFinder;
    private final MemberQueryRepository memberQueryRepository;
    private final MemberSystemRoleService memberSystemRoleService;
    private final SuperAdminGuard superAdminGuard;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminMemberSummary> listMembers(
            @Nullable MemberStatus status, @Nullable SystemRole role, @Nullable String keyword, Pageable pageable) {
        Specification<Member> spec = MemberSearchSpecs.forAdminDirectory(status, role, keyword);
        return memberQueryRepository.findAll(spec, pageable).map(AdminMemberSummary::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMemberDetail getMember(Long memberId) {
        Member member =
                memberQueryRepository.findById(memberId).orElseThrow(() -> new MemberNotFoundException(memberId));
        return AdminMemberDetail.from(member);
    }

    @Override
    public void changeSystemRole(Long targetMemberId, SystemRole newRole, Long actorMemberId) {
        memberSystemRoleService.changeSystemRole(actorMemberId, targetMemberId, newRole);
    }

    @Override
    public void forceWithdraw(Long targetMemberId, Long actorMemberId) {
        Member target = memberFinder.getActiveById(targetMemberId);
        superAdminGuard.ensureNotLastActiveSuperAdmin(target);
        target.withdraw();
        refreshTokenRepository.deleteByMemberId(targetMemberId);
    }

    @Override
    public void forceRestore(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        if (!target.isDeleted()) {
            throw new BadRequestException(MemberErrorCode.MEMBER_NOT_DELETED);
        }
        target.restore();
    }

    @Override
    public void revokeSessions(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        refreshTokenRepository.deleteByMemberId(target.getId());
    }
}
