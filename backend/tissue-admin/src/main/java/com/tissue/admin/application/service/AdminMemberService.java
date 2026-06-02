package com.tissue.admin.application.service;

import com.tissue.admin.adapter.persistence.MemberSearchSpecs;
import com.tissue.admin.application.dto.AdminMemberDetail;
import com.tissue.admin.application.dto.AdminMemberSummary;
import com.tissue.admin.application.port.usecase.AdminMemberUseCase;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.application.service.MemberFinder;
import com.tissue.feature.member.application.service.SuperAdminGuard;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.MemberErrorCode;
import com.tissue.feature.member.domain.exception.MemberNotFoundException;
import com.tissue.security.application.port.repository.RefreshTokenRepository;
import com.tissue.security.application.port.usecase.PasswordResetUseCase;
import com.tissue.security.application.service.MemberPurgeService;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.shared.meta.Evaluation;
import com.tissue.shared.meta.LLMGenerated;
import com.tissue.shared.meta.LLMInvolvement;
import java.util.Map;
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
        evaluationReason = "Passes human written integration test. Code was reviewed.",
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
    private final MemberPurgeService memberPurgeService;
    private final PasswordResetUseCase passwordResetUseCase;
    private final AdminAuditRecorder adminAuditRecorder;

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
        adminAuditRecorder.recordMemberAction(
                actorMemberId, AdminAuditAction.CHANGE_SYSTEM_ROLE, targetMemberId, Map.of("newRole", newRole.name()));
    }

    @Override
    public void forceWithdraw(Long targetMemberId, Long actorMemberId) {
        Member target = memberFinder.getActiveById(targetMemberId);
        superAdminGuard.ensureNotLastActiveSuperAdmin(target);
        target.withdraw();
        refreshTokenRepository.deleteByMemberId(targetMemberId);
        adminAuditRecorder.recordMemberAction(actorMemberId, AdminAuditAction.FORCE_WITHDRAW, targetMemberId, Map.of());
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
        adminAuditRecorder.recordMemberAction(actorMemberId, AdminAuditAction.FORCE_RESTORE, targetMemberId, Map.of());
    }

    @Override
    public void revokeSessions(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        refreshTokenRepository.deleteByMemberId(target.getId());
        adminAuditRecorder.recordMemberAction(
                actorMemberId, AdminAuditAction.REVOKE_SESSIONS, targetMemberId, Map.of());
    }

    @Override
    public void lockMember(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        if (target.isSuperAdmin()) {
            throw new ResourceConflictException(MemberErrorCode.CANNOT_LOCK_SUPER_ADMIN);
        }
        if (target.isLocked()) {
            throw new ResourceConflictException(MemberErrorCode.MEMBER_ALREADY_LOCKED);
        }
        if (!target.isActive()) {
            throw new BadRequestException(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
        target.lock();
        refreshTokenRepository.deleteByMemberId(targetMemberId);
        adminAuditRecorder.recordMemberAction(actorMemberId, AdminAuditAction.LOCK_MEMBER, targetMemberId, Map.of());
    }

    @Override
    public void unlockMember(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        if (!target.isLocked()) {
            throw new BadRequestException(MemberErrorCode.MEMBER_NOT_LOCKED);
        }
        target.unlock();
        adminAuditRecorder.recordMemberAction(actorMemberId, AdminAuditAction.UNLOCK_MEMBER, targetMemberId, Map.of());
    }

    @Override
    public void purgeMember(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        if (!target.isDeleted()) {
            throw new BadRequestException(MemberErrorCode.MEMBER_NOT_DELETED);
        }
        memberPurgeService.purge(target);
        adminAuditRecorder.recordMemberAction(actorMemberId, AdminAuditAction.PURGE_MEMBER, targetMemberId, Map.of());
    }

    @Override
    public void forcePasswordReset(Long targetMemberId, Long actorMemberId) {
        Member target = memberQueryRepository
                .findById(targetMemberId)
                .orElseThrow(() -> new MemberNotFoundException(targetMemberId));
        if (!target.isActive()) {
            throw new BadRequestException(MemberErrorCode.MEMBER_NOT_ACTIVE);
        }
        String email = target.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadRequestException(MemberErrorCode.MEMBER_NO_EMAIL);
        }
        passwordResetUseCase.requestPasswordReset(email);
        adminAuditRecorder.recordMemberAction(
                actorMemberId, AdminAuditAction.FORCE_PASSWORD_RESET, targetMemberId, Map.of());
    }
}
