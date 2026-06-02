package com.tissue.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tissue.admin.application.dto.AdminMemberDetail;
import com.tissue.admin.application.dto.AdminMemberSummary;
import com.tissue.admin.application.service.AdminMemberService;
import com.tissue.feature.member.application.port.repository.MemberCommandRepository;
import com.tissue.feature.member.application.port.repository.MemberQueryRepository;
import com.tissue.feature.member.domain.Member;
import com.tissue.feature.member.domain.MemberStatus;
import com.tissue.feature.member.domain.SystemRole;
import com.tissue.feature.member.domain.exception.LastSuperAdminException;
import com.tissue.feature.member.domain.exception.MemberNotFoundException;
import com.tissue.shared.exception.base.BadRequestException;
import com.tissue.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AdminMemberServiceIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private AdminMemberService adminMemberService;

    @Autowired
    private MemberCommandRepository memberCommandRepository;

    @Autowired
    private MemberQueryRepository memberQueryRepository;

    @Nested
    @DisplayName("list members")
    class ListMembers {

        @Test
        @DisplayName("filters by role and matches keyword across username/name/email")
        void filtersAndSearches() {
            // given
            memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super Admin"));
            memberCommandRepository.save(Member.create("alice@tissue.com", "alice", "Alice Kim"));
            memberCommandRepository.save(Member.create("bob@tissue.com", "bob", "Bob Lee"));
            em.flush();
            em.clear();

            // when - filter by USER role
            Page<AdminMemberSummary> users =
                    adminMemberService.listMembers(null, SystemRole.USER, null, PageRequest.of(0, 20));

            // then
            assertThat(users.getContent())
                    .extracting(AdminMemberSummary::username)
                    .containsExactlyInAnyOrder("alice", "bob");

            // when - keyword matches name
            Page<AdminMemberSummary> byName =
                    adminMemberService.listMembers(null, null, "alice ki", PageRequest.of(0, 20));

            // then
            assertThat(byName.getContent())
                    .extracting(AdminMemberSummary::username)
                    .containsExactly("alice");
        }

        @Test
        @DisplayName("includes withdrawn members and can filter by status")
        void includesWithdrawn() {
            // given
            Member active = memberCommandRepository.save(Member.create("active@tissue.com", "active", "Active"));
            Member gone = memberCommandRepository.save(Member.create("gone@tissue.com", "gone", "Gone"));
            gone.withdraw();
            memberCommandRepository.save(gone);
            em.flush();
            em.clear();

            // when
            Page<AdminMemberSummary> deleted =
                    adminMemberService.listMembers(MemberStatus.DELETED, null, null, PageRequest.of(0, 20));

            // then
            assertThat(deleted.getContent())
                    .extracting(AdminMemberSummary::username)
                    .containsExactly("gone");
        }
    }

    @Nested
    @DisplayName("get member")
    class GetMember {

        @Test
        @DisplayName("returns full detail for any member")
        void returnsDetail() {
            // given
            Member member = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            em.flush();
            em.clear();

            // when
            AdminMemberDetail detail = adminMemberService.getMember(member.getId());

            // then
            assertThat(detail.username()).isEqualTo("dev");
            assertThat(detail.role()).isEqualTo(SystemRole.USER);
            assertThat(detail.status()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("throws when member does not exist")
        void throwsWhenMissing() {
            assertThatThrownBy(() -> adminMemberService.getMember(999_999L))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("change system role by SUPER_ADMIN")
    class ChangeSystemRole {

        @Test
        @DisplayName("a SUPER_ADMIN promotes a USER to ADMIN")
        void promotesUser() {
            // given
            Member actor =
                    memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
            Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            em.flush();
            em.clear();

            // when
            adminMemberService.changeSystemRole(target.getId(), SystemRole.ADMIN, actor.getId());
            em.flush();
            em.clear();

            // then
            Member updated = memberQueryRepository.findById(target.getId()).orElseThrow();
            assertThat(updated.getRole()).isEqualTo(SystemRole.ADMIN);
        }
    }

    @Nested
    @DisplayName("force withdraw by SUPER_ADMIN")
    class ForceWithdraw {

        @Test
        @DisplayName("withdraws an active member")
        void withdraws() {
            // given
            Member actor =
                    memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
            Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            em.flush();
            em.clear();

            // when
            adminMemberService.forceWithdraw(target.getId(), actor.getId());
            em.flush();
            em.clear();

            // then
            Member updated = memberQueryRepository.findById(target.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(MemberStatus.DELETED);
            assertThat(updated.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("cannot withdraw the last active SUPER_ADMIN")
        void rejectsLastSuperAdmin() {
            // given
            Member onlySuper =
                    memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> adminMemberService.forceWithdraw(onlySuper.getId(), onlySuper.getId()))
                    .isInstanceOf(LastSuperAdminException.class);
        }
    }

    @Nested
    @DisplayName("force restore by SUPER_ADMIN")
    class ForceRestore {

        @Test
        @DisplayName("restores a withdrawn member")
        void restores() {
            // given
            Member actor =
                    memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
            Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            target.withdraw();
            memberCommandRepository.save(target);
            em.flush();
            em.clear();

            // when
            adminMemberService.forceRestore(target.getId(), actor.getId());
            em.flush();
            em.clear();

            // then
            Member updated = memberQueryRepository.findById(target.getId()).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(updated.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("rejects restoring a member that is not deleted")
        void rejectsNonDeleted() {
            // given
            Member actor =
                    memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
            Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            em.flush();
            em.clear();

            // when & then
            assertThatThrownBy(() -> adminMemberService.forceRestore(target.getId(), actor.getId()))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("revoke sessions by SUPER_ADMIN")
    class RevokeSessions {

        @Test
        @DisplayName("succeeds for an existing member (no-op when no active session)")
        void revokes() {
            // given
            Member actor =
                    memberCommandRepository.save(Member.createAsSuperAdmin("super@tissue.com", "super", "Super"));
            Member target = memberCommandRepository.save(Member.create("dev@tissue.com", "dev", "Dev"));
            em.flush();
            em.clear();

            // when & then
            assertThatCode(() -> adminMemberService.revokeSessions(target.getId(), actor.getId()))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("throws when member does not exist")
        void throwsWhenMissing() {
            assertThatThrownBy(() -> adminMemberService.revokeSessions(999_999L, 1L))
                    .isInstanceOf(MemberNotFoundException.class);
        }
    }
}
