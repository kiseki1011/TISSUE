package com.tissue.feature.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

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
}
