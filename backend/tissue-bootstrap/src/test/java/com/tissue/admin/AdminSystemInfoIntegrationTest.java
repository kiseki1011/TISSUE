package com.tissue.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.tissue.admin.application.dto.AdminSystemInfo;
import com.tissue.admin.application.service.AdminSystemInfoService;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminSystemInfoIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminSystemInfoService adminSystemInfoService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Test
    @DisplayName("summarizes member counts by status/role + instance config")
    void summarizesInstance() {
        // given
        memberCommandRepository.save(Member.createAsSuperAdmin("su@tissue.com", "su", "Su"));
        memberCommandRepository.save(Member.createAsAdmin("admin@tissue.com", "admin", "Admin"));
        memberCommandRepository.save(Member.create("user1@tissue.com", "user1", "User1"));
        memberCommandRepository.save(Member.create("user2@tissue.com", "user2", "User2"));
        Member withdrawn = Member.create("gone@tissue.com", "gone", "Gone");
        withdrawn.withdraw();
        memberCommandRepository.save(withdrawn);
        em.flush();
        em.clear();

        // when
        AdminSystemInfo info = adminSystemInfoService.getSystemInfo();

        // then
        assertThat(info.version()).isNotBlank();
        assertThat(info.activeProfiles()).contains("test");
        assertThat(info.seeded()).isTrue();
        assertThat(info.members().total()).isEqualTo(5);
        assertThat(info.members().active()).isEqualTo(4);
        assertThat(info.members().deleted()).isEqualTo(1);
        assertThat(info.members().locked()).isZero();
        assertThat(info.members().purged()).isZero();
        assertThat(info.members().activeSuperAdmins()).isEqualTo(1);
    }
}
