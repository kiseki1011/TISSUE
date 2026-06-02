package com.tissue.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.admin.application.dto.AdminAuditLogResponse;
import com.tissue.admin.application.service.AdminAuditQueryService;
import com.tissue.admin.application.service.AdminMemberService;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.shared.exception.base.ResourceConflictException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminMemberLifecycleIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminMemberService adminMemberService;

    @Autowired
    private AdminAuditQueryService adminAuditQueryService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    private static final Long ACTOR = 999L;

    private Member reload(Long id) {
        em.flush();
        em.clear();
        return memberQueryRepository.findById(id).orElseThrow();
    }

    @Nested
    @DisplayName("lock & unlock member")
    class LockUnlock {

        @Test
        @DisplayName("locks an active member, revokes sessions, and audits")
        void locksActiveMember() {
            Member target = memberCommandRepository.save(Member.create("user@tissue.com", "user", "User"));
            em.flush();
            em.clear();

            adminMemberService.lockMember(target.getId(), ACTOR);

            assertThat(reload(target.getId()).getStatus()).isEqualTo(MemberStatus.LOCKED);
            Page<AdminAuditLogResponse> audit = adminAuditQueryService.listAuditLogs(
                    null, AdminAuditAction.LOCK_MEMBER, null, PageRequest.of(0, 5));
            assertThat(audit.getContent()).hasSize(1);
            assertThat(audit.getContent().getFirst().targetRef()).isEqualTo(String.valueOf(target.getId()));
        }

        @Test
        @DisplayName("unlocks a locked member back to active")
        void unlocksMember() {
            Member target = memberCommandRepository.save(Member.create("user@tissue.com", "user", "User"));
            em.flush();
            em.clear();
            adminMemberService.lockMember(target.getId(), ACTOR);
            em.flush();
            em.clear();

            adminMemberService.unlockMember(target.getId(), ACTOR);

            assertThat(reload(target.getId()).getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("cannot lock a SUPER_ADMIN")
        void cannotLockSuperAdmin() {
            Member su = memberCommandRepository.save(Member.createAsSuperAdmin("suser@tissue.com", "suser", "Suser"));
            em.flush();
            em.clear();

            assertThatThrownBy(() -> adminMemberService.lockMember(su.getId(), ACTOR))
                    .isInstanceOf(ResourceConflictException.class);
        }

        @Test
        @DisplayName("cannot lock an already locked member")
        void cannotLockAlreadyLocked() {
            Member target = memberCommandRepository.save(Member.create("user@tissue.com", "user", "User"));
            em.flush();
            em.clear();
            adminMemberService.lockMember(target.getId(), ACTOR);
            em.flush();
            em.clear();

            assertThatThrownBy(() -> adminMemberService.lockMember(target.getId(), ACTOR))
                    .isInstanceOf(ResourceConflictException.class);
        }

        @Test
        @DisplayName("cannot unlock a member that is not locked")
        void cannotUnlockNotLocked() {
            Member target = memberCommandRepository.save(Member.create("user@tissue.com", "user", "User"));
            em.flush();
            em.clear();

            assertThatThrownBy(() -> adminMemberService.unlockMember(target.getId(), ACTOR))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("purge member")
    class Purge {

        @Test
        @DisplayName("anonymizes a deleted member and audits")
        void purgesDeletedMember() {
            Member target = Member.create("gone@tissue.com", "gone", "Gone");
            target.withdraw();
            memberCommandRepository.save(target);
            Long id = target.getId();
            em.flush();
            em.clear();

            adminMemberService.purgeMember(id, ACTOR);

            Member purged = reload(id);
            assertThat(purged.getStatus()).isEqualTo(MemberStatus.PURGED);
            assertThat(purged.getEmail()).isNull();
            assertThat(purged.getUsername()).isEqualTo("deleted_" + id);

            Page<AdminAuditLogResponse> audit = adminAuditQueryService.listAuditLogs(
                    null, AdminAuditAction.PURGE_MEMBER, null, PageRequest.of(0, 5));
            assertThat(audit.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("rejects purging a member that is not deleted")
        void rejectsNonDeleted() {
            Member target = memberCommandRepository.save(Member.create("user@tissue.com", "user", "User"));
            em.flush();
            em.clear();

            assertThatThrownBy(() -> adminMemberService.purgeMember(target.getId(), ACTOR))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("force password reset")
    class ForcePasswordReset {

        @Test
        @DisplayName("triggers a reset for an active member with an email and audits")
        void triggersReset() {
            Member target = memberCommandRepository.save(Member.create("user@tissue.com", "user", "User"));
            em.flush();
            em.clear();

            adminMemberService.forcePasswordReset(target.getId(), ACTOR);

            Page<AdminAuditLogResponse> audit = adminAuditQueryService.listAuditLogs(
                    null, AdminAuditAction.FORCE_PASSWORD_RESET, null, PageRequest.of(0, 5));
            assertThat(audit.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("rejects a member without an email")
        void rejectsNoEmail() {
            Member target = memberCommandRepository.save(Member.createWithoutEmail("noemail", "No Email"));
            em.flush();
            em.clear();

            assertThatThrownBy(() -> adminMemberService.forcePasswordReset(target.getId(), ACTOR))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("rejects a non active member")
        void rejectsNonActive() {
            Member target = Member.create("gone@tissue.com", "gone", "Gone");
            target.withdraw();
            memberCommandRepository.save(target);
            em.flush();
            em.clear();

            assertThatThrownBy(() -> adminMemberService.forcePasswordReset(target.getId(), ACTOR))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
