package com.tissue.feature.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MemberTest {

    @Nested
    @DisplayName("withdraw()")
    class Withdraw {

        @Test
        @DisplayName("sets status as DELETED and deletedAt to current time")
        void marksDeletedWithTimestamp() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");
            Instant before = Instant.now();

            // when
            member.withdraw();

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.DELETED);
            assertThat(member.isDeleted()).isTrue();
            assertThat(member.getDeletedAt()).isNotNull().isAfterOrEqualTo(before);
        }
    }

    @Nested
    @DisplayName("anonymize()")
    class Anonymize {

        @Test
        @DisplayName("wipes PII fields and set status to PURGED")
        void clearsPiiAndPurges() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");
            member.withdraw();

            // when
            member.anonymize();

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.PURGED);
            assertThat(member.isPurged()).isTrue();
            assertThat(member.getEmail()).isNull();
            assertThat(member.getName()).isEqualTo("Deleted User");
            assertThat(member.getUsername()).startsWith("deleted_");
        }

        @Test
        @DisplayName("preserves deletedAt")
        void preservesDeletedAt() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");
            member.withdraw();
            Instant deletedAt = member.getDeletedAt();

            // when
            member.anonymize();

            // then
            assertThat(member.getDeletedAt()).isEqualTo(deletedAt);
        }
    }

    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("set status back to ACTIVE and clears deletedAt")
        void restoresFromDeleted() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");
            member.withdraw();

            // when
            member.restore();

            // then
            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.isActive()).isTrue();
            assertThat(member.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("rejects restoring if current status is ACTIVE")
        void rejectsActive() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");

            // when & then
            assertThatThrownBy(member::restore).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejects restoring if current status is PURGED")
        void rejectsPurged() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");
            member.withdraw();
            member.anonymize();

            // when & then
            assertThatThrownBy(member::restore).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("createAsSuperAdmin()")
    class CreateAsSuperAdmin {

        @Test
        @DisplayName("creates an active member with SUPER_ADMIN role")
        void createsSuperAdmin() {
            // when
            Member member = Member.createAsSuperAdmin("super@tissue.com", "super", "Super Admin");

            // then
            assertThat(member.getRole()).isEqualTo(SystemRole.SUPER_ADMIN);
            assertThat(member.isSuperAdmin()).isTrue();
            assertThat(member.isActive()).isTrue();
        }
    }

    @Nested
    @DisplayName("changeRole()")
    class ChangeRole {

        @Test
        @DisplayName("changes the system role")
        void changesRole() {
            // given
            Member member = Member.create("gildong@tissue.com", "gildong", "Gildong Hong");

            // when
            member.changeRole(SystemRole.ADMIN);

            // then
            assertThat(member.getRole()).isEqualTo(SystemRole.ADMIN);
            assertThat(member.isSuperAdmin()).isFalse();
        }
    }

    @Nested
    @DisplayName("hasAtLeast()")
    class HasAtLeast {

        @Test
        @DisplayName("is true when the role meets or exceeds the required role")
        void meetsOrExceeds() {
            // given
            Member superAdmin = Member.createAsSuperAdmin("a@tissue.com", "a", "A");
            Member admin = Member.createAsAdmin("b@tissue.com", "b", "B");
            Member user = Member.create("c@tissue.com", "c", "C");

            // when & then
            assertThat(superAdmin.hasAtLeast(SystemRole.ADMIN)).isTrue();
            assertThat(admin.hasAtLeast(SystemRole.ADMIN)).isTrue();
            assertThat(user.hasAtLeast(SystemRole.ADMIN)).isFalse();
        }
    }
}
