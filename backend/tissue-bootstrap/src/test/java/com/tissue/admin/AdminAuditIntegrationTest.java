package com.tissue.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.admin.application.dto.AdminAuditLogResponse;
import com.tissue.admin.application.service.AdminAuditQueryService;
import com.tissue.admin.application.service.AdminMemberService;
import com.tissue.admin.domain.AdminAuditAction;
import com.tissue.admin.domain.AdminAuditTargetType;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminAuditIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminMemberService adminMemberService;

    @Autowired
    private AdminAuditQueryService adminAuditQueryService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Test
    @DisplayName("a role change writes a CHANGE_SYSTEM_ROLE audit entry for the target")
    void roleChangeIsAudited() {
        // given
        Member actor = memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
        Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
        em.flush();
        em.clear();

        // when
        adminMemberService.changeSystemRole(target.getId(), SystemRole.ADMIN, actor.getId());
        em.flush();
        em.clear();

        // then
        Page<AdminAuditLogResponse> audit =
                adminAuditQueryService.listAuditLogs(null, null, null, PageRequest.of(0, 20));
        assertThat(audit.getContent()).hasSize(1);

        AdminAuditLogResponse entry = audit.getContent().getFirst();
        assertThat(entry.action()).isEqualTo(AdminAuditAction.CHANGE_SYSTEM_ROLE);
        assertThat(entry.targetType()).isEqualTo(AdminAuditTargetType.MEMBER);
        assertThat(entry.targetRef()).isEqualTo(String.valueOf(target.getId()));
        assertThat(entry.actorMemberId()).isEqualTo(actor.getId());
        assertThat(entry.data()).containsEntry("newRole", "ADMIN");
    }

    @Test
    @DisplayName("filters the audit log by action")
    void filtersByAction() {
        // given
        Member actor = memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
        Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
        em.flush();
        em.clear();

        adminMemberService.revokeSessions(target.getId(), actor.getId());
        adminMemberService.changeSystemRole(target.getId(), SystemRole.ADMIN, actor.getId());
        em.flush();
        em.clear();

        // when
        Page<AdminAuditLogResponse> revoked = adminAuditQueryService.listAuditLogs(
                null, AdminAuditAction.REVOKE_SESSIONS, null, PageRequest.of(0, 20));

        // then
        assertThat(revoked.getContent()).hasSize(1);
        assertThat(revoked.getContent().getFirst().action()).isEqualTo(AdminAuditAction.REVOKE_SESSIONS);
    }
}
